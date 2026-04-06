package com.Accommodation.controller;

import com.Accommodation.service.S3FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class ImageController {

    private final S3FileService s3FileService;

    @GetMapping("/images/s3")
    public ResponseEntity<byte[]> getImage(
            @RequestParam("key") String key
    ) {
        try {
            ResponseBytes<GetObjectResponse> fileBytes =
                    s3FileService.getFileBytes(key);

            String contentType =
                    fileBytes.response().contentType();

            MediaType mediaType =
                    contentType == null || contentType.isBlank()
                            ? MediaType.APPLICATION_OCTET_STREAM
                            : MediaType.parseMediaType(contentType);

            return ResponseEntity.ok()
                    .cacheControl(
                            CacheControl.maxAge(Duration.ofMinutes(10))
                    )
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "inline"
                    )
                    .contentType(mediaType)
                    .body(fileBytes.asByteArray());
        } catch (S3Exception e) {
            String message =
                    "S3 image load failed: "
                            + e.awsErrorDetails().errorCode()
                            + " - "
                            + e.awsErrorDetails().errorMessage();

            return ResponseEntity.status(e.statusCode())
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(message.getBytes());
        } catch (Exception e) {
            String message =
                    "Image proxy failed: "
                            + (e.getMessage() == null
                            ? e.getClass().getSimpleName()
                            : e.getMessage());

            return ResponseEntity.internalServerError()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(message.getBytes());
        }
    }
}
