package com.example.sb10_MoPl_team3.global.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@ConditionalOnProperty(prefix = "aws.s3", name = {"bucket", "region"})
public class S3Config {

	// 업로드한 파일을 저장/조회할 버킷을 지정하기 위해 필요
	@Value("${aws.s3.bucket}")
	private String bucket;

	// S3Client가 요청을 보낼 리전을 결정하기 위해 필요 (리전이 다르면 엔드포인트가 달라짐)
	@Value("${aws.s3.region}")
	private String region;

	@Bean
	public S3Client s3Client() {
		return S3Client.builder()
			.region(Region.of(region)).credentialsProvider(DefaultCredentialsProvider.create())
			.build();
	}
}
