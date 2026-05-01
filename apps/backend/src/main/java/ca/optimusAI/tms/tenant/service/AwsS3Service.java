package ca.optimusAI.tms.tenant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;

/**
 * Handles file uploads to AWS S3.
 * Used for tenant logo uploads and report file storage.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AwsS3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket:tms-reports}")
    private String bucket;

    @Value("${aws.region:ca-central-1}")
    private String region;

    /**
     * Uploads a logo file to S3 under tenants/{tenantId}/logo.{ext}
     *
     * @return the public HTTPS URL of the uploaded logo
     */
    public String uploadTenantLogo(UUID tenantId, MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String ext = resolveExtension(originalFilename);
        validateLogoFile(file, ext);

        String key = "tenants/" + tenantId + "/logo." + ext;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

        String url = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
        log.info("Uploaded tenant logo for tenantId={} to {}", tenantId, url);
        return url;
    }

    /**
     * Uploads arbitrary bytes to S3 and returns the object URL.
     * Used by report generation workers.
     */
    public String uploadBytes(String key, byte[] content, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .contentLength((long) content.length)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(content));

        String url = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
        log.debug("Uploaded {} bytes to S3 key={}", content.length, key);
        return url;
    }

    /**
     * Deletes an object from S3. Failures are logged but not rethrown.
     */
    public void delete(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to delete S3 key={}: {}", key, e.getMessage());
        }
    }

    /**
     * Generates a presigned GET URL valid for {@code validity}.
     * Used by the report worker to produce time-limited download links.
     */
    public String generatePresignedUrl(String key, Duration validity) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(validity)
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build())
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toExternalForm();
    }

    /** Extracts just the key path from a full S3 URL. */
    public String keyFromUrl(String url) {
        try {
            String path = URI.create(url).getPath();
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (Exception e) {
            return url;
        }
    }

    private String resolveExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "png";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private void validateLogoFile(MultipartFile file, String ext) {
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new IllegalArgumentException("Logo file must be 2 MB or less");
        }
        if (!ext.matches("png|jpg|jpeg|svg")) {
            throw new IllegalArgumentException("Logo must be PNG, JPG, or SVG");
        }
    }
}
