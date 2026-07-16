# k6 + Prometheus + Grafana

## 모니터링 환경 실행

애플리케이션은 기본적으로 `dev` 프로필로 실행되며,
`/actuator/prometheus`에서 메트릭을 제공합니다.

```bash
docker compose up -d --build
```

- 애플리케이션: http://localhost:8080
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000
- Grafana 기본 계정: `admin` / `admin`

Prometheus는 애플리케이션 메트릭을 5초마다 수집합니다. Grafana에는
Prometheus 데이터소스와 `MoPl / MoPl Load Test Overview` 대시보드가
자동으로 등록됩니다.

## k6 결과를 Prometheus로 전송

k6는 로컬 PC에 별도로 설치해 실행합니다. 각 실행을 구분할 수 있도록
`testid`에는 고유한 값을 사용합니다.

```bash
K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write \
K6_PROMETHEUS_RW_TREND_STATS=p(95),p(99),min,max \
k6 run -o experimental-prometheus-rw \
  --tag testid=smoke-$(date +%Y%m%d-%H%M%S) \
  k6/smoke.js
```

모든 k6 메트릭은 `k6_` 접두사로 저장되며 Prometheus 보존 기간은 15일입니다.
Remote Write 엔드포인트는 로컬 테스트 전용이므로 외부에 공개하지 않습니다.

## 종료

수집한 메트릭과 Grafana 데이터를 유지하면서 종료합니다.

```bash
docker compose down
```

모든 볼륨까지 삭제합니다.

```bash
docker compose down -v
```

이 명령은 PostgreSQL, Redis, Kafka 데이터도 함께 삭제하므로 주의해야 합니다.
