package com.example.sb10_MoPl_team3.global.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

	// 업로드한 파일을 저장/조회할 버킷을 지정하기 위해 필요
	@Value("${aws.s3.bucket}")
	private String bucket;

	// S3Client가 요청을 보낼 리전을 결정하기 위해 필요 (리전이 다르면 엔드포인트가 달라짐)
	@Value("${aws.s3.region}")
	private String region;

	// S3 API 호출을 인증하기 위한 액세스 키
	@Value("${aws.s3.credentials.access-key}")
	private String accessKey;

	// 액세스 키와 한 쌍으로 요청 서명에 사용되는 시크릿 키
	@Value("${aws.s3.credentials.secret-key}")
	private String secretKey;

	@Bean
	public S3Client s3Client() {
		// 매번 자격 증명을 다시 조회하지 않도록 고정된(Static) 자격 증명을 사용
		AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

		return S3Client.builder()
			.region(Region.of(region))
			.credentialsProvider(StaticCredentialsProvider.create(credentials))
			.build();
	}

	public String getBucket() {
		return bucket;
	}
}
