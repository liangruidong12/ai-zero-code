package com.wait.aizerocode.utils;

import cn.hutool.core.util.StrUtil;
import com.wait.aizerocode.config.MinioConfig;
import io.minio.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.util.UUID;

@Component
public class MinioUtils {

    @Resource
    private MinioClient minioClient;

    @Resource
    private MinioConfig minioConfig;

    /**
     * Spring Boot 启动时检查 Bucket
     */
    @PostConstruct
    public void init() {
        try {

            String bucketName = minioConfig.getBucketName();

            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            if (!exists) {

                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                );

                System.out.println(
                        "MinIO Bucket 创建成功：" + bucketName
                );
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    "MinIO Bucket 初始化失败",
                    e
            );
        }
    }

    /**
     * 上传本地文件到 MinIO
     *
     * @param filePath   本地文件路径
     * @param objectName MinIO 中的对象名称，例如 screenshot/2026/09/17/xxx.png
     * @return 文件访问 URL
     */
    public String uploadFile(String filePath, String objectName) {

        if (StrUtil.isBlank(filePath)) {
            throw new IllegalArgumentException("本地文件路径不能为空");
        }

        if (StrUtil.isBlank(objectName)) {
            throw new IllegalArgumentException("MinIO 对象名称不能为空");
        }

        try {

            File file = new File(filePath);

            if (!file.exists()) {
                throw new IllegalArgumentException(
                        "本地文件不存在：" + filePath
                );
            }

            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .filename(file.getAbsolutePath())
                            .build()
            );

            return minioConfig.getEndpoint()
                    + "/"
                    + minioConfig.getBucketName()
                    + "/"
                    + objectName;

        } catch (Exception e) {

            throw new RuntimeException(
                    "MinIO 文件上传失败：" + filePath,
                    e
            );
        }
    }

    /**
     * 删除文件
     */
    public void delete(String objectName) {

        try {

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(
                                    minioConfig
                                            .getBucketName()
                            )
                            .object(objectName)
                            .build()
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "MinIO 文件删除失败",
                    e
            );
        }
    }

    /**
     * 获取文件
     */
    public InputStream getObject(String objectName) {

        try {

            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(
                                    minioConfig
                                            .getBucketName()
                            )
                            .object(objectName)
                            .build()
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "MinIO 文件读取失败",
                    e
            );
        }
    }
}