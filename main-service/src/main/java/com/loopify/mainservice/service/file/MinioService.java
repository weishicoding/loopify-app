package com.loopify.mainservice.service.file;

import jakarta.annotation.PostConstruct;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@Service
public class MinioService {
    private S3Client s3Client;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.bucket}")
    private String bucketName;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    public MinioService() {
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create("http://minio:9000")) // 初始值，@PostConstruct 会覆盖
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("minioadmin", "minioadmin")))
                .region(Region.US_EAST_1) // MinIO 不严格需要区域，但 SDK 要求
                .build();
    }

    @PostConstruct
    public void init() {
        // 更新 endpoint（从配置文件读取）
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.US_EAST_1)
                .build();

        // 创建 bucket（如果不存在）
        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        } catch (Exception e) {
            // Bucket 已存在，忽略
        }
    }

    public String uploadFile(MultipartFile file, String folder, boolean isAvatar) throws IOException {
        // 压缩图片
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        if (isAvatar) {
            // 头像：200x200，质量 0.7
            Thumbnails.of(file.getInputStream())
                    .size(200, 200)
                    .outputQuality(0.7)
                    .outputFormat("jpg")
                    .toOutputStream(outputStream);
        } else {
            // 产品图片：800x600，质量 0.8
            Thumbnails.of(file.getInputStream())
                    .size(800, 600)
                    .outputQuality(0.8)
                    .outputFormat("jpg")
                    .toOutputStream(outputStream);
        }
        String fileName = folder + "/" + UUID.randomUUID() + ".jpg";
        byte[] compressedBytes = outputStream.toByteArray();

        // 上传到 MinIO
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();
        s3Client.putObject(request, RequestBody.fromInputStream(
                new ByteArrayInputStream(compressedBytes), compressedBytes.length));

        return endpoint + "/" + bucketName + "/" + fileName;
    }

    public void deleteFile(String fileUrl) {
        String key = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        s3Client.deleteObject(builder -> builder.bucket(bucketName).key(key));
    }
}
