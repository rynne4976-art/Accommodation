package com.Accommodation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3FileService {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region}")
    private String region;

    // =========================
    // 업로드
    // =========================
    public String uploadFile(
            String dirName,
            String originalFileName,
            MultipartFile multipartFile
    ) throws Exception {

        String savedFileName =
                createFileName(originalFileName);

        String key =
                dirName + "/" + savedFileName;

        PutObjectRequest putObjectRequest =
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(multipartFile.getContentType())
                        .build();

        try (InputStream inputStream =
                     multipartFile.getInputStream()) {

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(
                            inputStream,
                            multipartFile.getSize()
                    )
            );
        }

        return key;
    }

    // =========================
    // 삭제
    // =========================
    public void deleteFile(String key) {

        if (key == null || key.isBlank()) {
            return;
        }

        DeleteObjectRequest request =
                DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build();

        s3Client.deleteObject(request);
    }

    // =========================
    // URL 생성
    // =========================
    public String getFileUrl(String key) {

        String encodedKey =
                URLEncoder.encode(
                                key,
                                StandardCharsets.UTF_8
                        )
                        .replace("+", "%20")
                        .replace("%2F", "/");

        return "https://"
                + bucket
                + ".s3."
                + region
                + ".amazonaws.com/"
                + encodedKey;
    }

    public String getProxyImageUrl(String key) {

        if (key == null || key.isBlank()) {
            return "";
        }

        return "/images/s3?key="
                + URLEncoder.encode(
                        key,
                        StandardCharsets.UTF_8
                );
    }

    public ResponseBytes<GetObjectResponse> getFileBytes(
            String key
    ) {

        GetObjectRequest request =
                GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build();

        return s3Client.getObjectAsBytes(request);
    }

    // =========================
    // 파일명 생성
    // =========================
    private String createFileName(
            String originalFileName
    ) {

        String extension =
                extractExtension(originalFileName);

        return UUID.randomUUID()
                + "."
                + extension;
    }

    private String extractExtension(
            String originalFileName
    ) {

        int pos =
                originalFileName.lastIndexOf(".");

        if (pos == -1) {
            throw new IllegalArgumentException(
                    "파일 확장자가 없습니다."
            );
        }

        return originalFileName.substring(pos + 1);
    }
}
