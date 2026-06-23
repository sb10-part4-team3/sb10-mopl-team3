package com.example.sb10_MoPl_team3.global.file;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;

@Service
@ConditionalOnProperty(prefix = "aws.s3", name = {"bucket", "region"})
public class FileStorageService {

  private final S3Client s3Client;

  @Value("${aws.s3.bucket}")
  private String bucket;

  @Value("${aws.s3.region}")
  private String region;

  public FileStorageService(S3Client s3Client) {
    this.s3Client = s3Client;
  }

  public String upload(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("업로드할 파일이 없습니다.");
    }

    long maxSize = 10 * 1024 * 1024; // 10MB
    if (file.getSize() > maxSize) {
      throw new IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다.");
    }
    try {
      // 1. 파일명 중복 방지를 위한 key 생성
      String originalFilename = file.getOriginalFilename();
      String extension = originalFilename != null && originalFilename.contains(".")
          ? originalFilename.substring(originalFilename.lastIndexOf("."))
          : "";
      if (!extension.matches("\\.[A-Za-z0-9]{1,10}")) {
        extension = "";
      }

      String key = UUID.randomUUID() + extension;
      // 2. S3에 보낼 요청 객체 생성 (어느 버킷의 어떤 key에 저장할지)
      PutObjectRequest request = PutObjectRequest.builder()
          .bucket(bucket)
          .key(key)
          .contentType(file.getContentType())
          .build();

      // 3. 파일의 바이트 데이터를 S3가 읽을 수 있는 형태(RequestBody)로 변환
      RequestBody body = RequestBody.fromBytes(file.getBytes());

      // 4. 실제 업로드 실행
      s3Client.putObject(request, body);

      // 5. 업로드된 객체에 접근할 수 있는 URL 조립해서 반환
      return s3Client.utilities()
          .getUrl(GetUrlRequest.builder().bucket(bucket).key(key).build())
          .toExternalForm();

    } catch (Exception e) {
      throw new RuntimeException("파일 업로드에 실패했습니다.", e);
    }
  }

}