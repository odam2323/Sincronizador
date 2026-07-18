package org.example.sincronizador2.services.impl;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.sincronizador2.minio.MinioConfig;
import org.example.sincronizador2.services.MinioService;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class MinioServiceImpl implements MinioService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @PostConstruct
    public void initBucket() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(minioConfig.getBucket()).build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(minioConfig.getBucket()).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error inicializando bucket de MinIO", e);
        }
    }

    @Override
    public void uploadFile(String objectName, InputStream stream, long size, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(objectName)
                    .stream(stream, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Error subiendo archivo a MinIO: " + e.getMessage(), e);
        }
    }
}