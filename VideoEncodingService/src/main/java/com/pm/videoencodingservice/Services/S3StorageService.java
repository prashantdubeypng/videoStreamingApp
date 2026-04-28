package com.pm.videoencodingservice.Services;

import com.pm.videoencodingservice.config.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Handles all S3 interactions: streaming download of raw video,
 * and recursive upload of the HLS output directory.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService {

    private static final int STREAM_BUFFER_SIZE = 8 * 1024 * 1024; // 8 MB

    private final S3Client s3Client;
    private final AwsProperties awsProperties;

    /**
     * Stream-downloads the raw video from S3 to a local temp file.
     * Never loads the full file into memory.
     *
     * @param s3Key  the object key in S3
     * @param target local file path to write to
     */
    public void downloadToFile(String s3Key, Path target) {
        log.info("⬇ Downloading s3://{}/{} → {}", awsProperties.getS3Bucket(), s3Key, target);

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(awsProperties.getS3Bucket())
                .key(s3Key)
                .build();

        try (InputStream s3Stream = s3Client.getObject(request);
             OutputStream fileOut = Files.newOutputStream(target)) {

            byte[] buffer = new byte[STREAM_BUFFER_SIZE];
            long totalBytes = 0;
            int bytesRead;

            while ((bytesRead = s3Stream.read(buffer)) != -1) {
                fileOut.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }

            log.info("⬇ Download complete: {} bytes written to {}", totalBytes, target);

        } catch (IOException e) {
            throw new RuntimeException("Failed to download s3://" + awsProperties.getS3Bucket() + "/" + s3Key, e);
        }
    }

    /**
     * Recursively uploads all files in {@code localDir} to S3 under {@code s3Prefix}.
     * Preserves relative directory structure.
     *
     * @param localDir the local directory containing HLS output
     * @param s3Prefix e.g. "videos/{videoId}/hls/"
     * @return total number of files uploaded
     */
    public int uploadDirectory(Path localDir, String s3Prefix) {
        log.info("⬆ Uploading directory {} → s3://{}/{}", localDir, awsProperties.getS3Bucket(), s3Prefix);

        int[] count = {0};

        try {
            Files.walkFileTree(localDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String relativePath = localDir.relativize(file).toString().replace("\\", "/");
                    String s3Key = s3Prefix + relativePath;
                    String contentType = resolveContentType(file.getFileName().toString());

                    PutObjectRequest putRequest = PutObjectRequest.builder()
                            .bucket(awsProperties.getS3Bucket())
                            .key(s3Key)
                            .contentType(contentType)
                            .build();

                    s3Client.putObject(putRequest, RequestBody.fromFile(file));
                    count[0]++;

                    if (count[0] % 50 == 0) {
                        log.debug("⬆ Uploaded {} files so far...", count[0]);
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload directory to S3: " + s3Prefix, e);
        }

        log.info("⬆ Upload complete: {} files to s3://{}/{}", count[0], awsProperties.getS3Bucket(), s3Prefix);
        return count[0];
    }

    private String resolveContentType(String fileName) {
        if (fileName.endsWith(".m3u8")) return "application/vnd.apple.mpegurl";
        if (fileName.endsWith(".ts"))   return "video/mp2t";
        if (fileName.endsWith(".mp4"))  return "video/mp4";
        return "application/octet-stream";
    }
}
