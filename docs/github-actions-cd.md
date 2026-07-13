# GitHub Actions CD 설정

`dev` 브랜치에 push되면 기존 테스트가 성공한 뒤 Docker 이미지를 ECR에 push하고 ECS 서비스를 갱신한다. 애플리케이션의 DB, Redis, JWT, S3, SMTP 비밀값은 GitHub에 복사하지 않고 ECS 태스크 정의의 `secrets` 또는 환경 변수에 둔다.

## 1. GitHub Environment

저장소의 **Settings > Environments**에서 `dev` environment를 만든 뒤 다음 값을 등록한다.

`dev` environment의 **Deployment branches and tags**를 **Selected branches and tags**로 설정하고 `dev` 브랜치만 허용한다. 이 설정은 필수다. Environment를 사용하는 OIDC `sub` claim에는 브랜치가 포함되지 않으므로, 이 보호 규칙이 없으면 다른 브랜치의 워크플로도 `environment: dev`를 지정해 배포 Role을 맡을 수 있다.

Environment secret:

| 이름 | 값 |
| --- | --- |
| `AWS_ROLE_ARN` | GitHub Actions가 OIDC로 맡을 IAM Role ARN |

Environment variables:

| 이름 | 예시 |
| --- | --- |
| `AWS_REGION` | `ap-northeast-2` |
| `ECR_REPOSITORY` | `mopl-repo` |
| `ECS_CLUSTER` | ECS 클러스터 이름 |
| `ECS_SERVICE` | ECS 서비스 이름 |
| `ECS_TASK_DEFINITION` | ECS 태스크 정의 family 이름 |
| `ECS_CONTAINER_NAME` | 태스크 정의의 애플리케이션 container `name` |

## 2. AWS OIDC Role

AWS IAM에 GitHub OIDC provider `token.actions.githubusercontent.com`을 만들고 audience를 `sts.amazonaws.com`으로 지정한다. 아래 Role 신뢰 정책은 저장소와 `dev` GitHub Environment를 제한하지만 브랜치는 제한하지 않는다. `dev` 브랜치 제한은 앞에서 설정한 GitHub Environment의 **Deployment branches and tags** 보호 규칙이 담당한다.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::<AWS_ACCOUNT_ID>:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
          "token.actions.githubusercontent.com:sub": "repo:sb10-part4-team3/sb10-mopl-team3:environment:dev"
        }
      }
    }
  ]
}
```

Role 권한 정책에는 최소한 다음 작업이 필요하다. `<...>`를 실제 ARN으로 교체한다.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "EcrAuth",
      "Effect": "Allow",
      "Action": "ecr:GetAuthorizationToken",
      "Resource": "*"
    },
    {
      "Sid": "PushImage",
      "Effect": "Allow",
      "Action": [
        "ecr:BatchCheckLayerAvailability",
        "ecr:CompleteLayerUpload",
        "ecr:InitiateLayerUpload",
        "ecr:PutImage",
        "ecr:UploadLayerPart"
      ],
      "Resource": "arn:aws:ecr:<REGION>:<AWS_ACCOUNT_ID>:repository/<ECR_REPOSITORY>"
    },
    {
      "Sid": "DeployEcs",
      "Effect": "Allow",
      "Action": [
        "ecs:DescribeServices",
        "ecs:DescribeTaskDefinition",
        "ecs:RegisterTaskDefinition",
        "ecs:UpdateService"
      ],
      "Resource": "*"
    },
    {
      "Sid": "PassTaskRoles",
      "Effect": "Allow",
      "Action": "iam:PassRole",
      "Resource": [
        "arn:aws:iam::<AWS_ACCOUNT_ID>:role/<ECS_TASK_EXECUTION_ROLE>",
        "arn:aws:iam::<AWS_ACCOUNT_ID>:role/<ECS_TASK_ROLE>"
      ]
    }
  ]
}
```

## 3. 최초 배포 전 확인

- ECR repository, ECS cluster/service, ECS task definition이 먼저 존재해야 한다.
- 태스크 정의의 앱 컨테이너 port는 `8080`이어야 한다.
- ALB health check는 `/actuator/health`를 사용한다.
- ECS task execution role은 ECR pull 및 CloudWatch Logs 권한을 가진다.
- 애플리케이션 런타임 환경 변수는 ECS 태스크 정의에 설정한다. 특히 `SPRING_PROFILES_ACTIVE=prod`, DB/Redis 접속값, `JWT_SECRET`, 관리자 계정, S3/TMDB/SMTP 설정이 필요하다.
- 브랜치 제한과 별도로 Required reviewers 같은 protection rule을 추가하면 승인 후에만 배포하도록 구성할 수 있다.

워크플로는 기존 태스크 정의를 AWS에서 내려받아 앱 컨테이너의 image만 commit SHA 태그로 교체한다. 따라서 새 배포가 실패하면 ECS에서 이전 태스크 정의 revision으로 되돌릴 수 있다.
