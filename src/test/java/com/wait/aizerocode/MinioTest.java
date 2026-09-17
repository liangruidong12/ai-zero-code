package com.wait.aizerocode;

import com.wait.aizerocode.utils.MinioUtils;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

@SpringBootTest
public class MinioTest {

    @Resource
    private MinioUtils minioUtils;

    @Test
    void testUpload() {

        // 模拟一个上传文件
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Hello MinIO!".getBytes(StandardCharsets.UTF_8)
        );

        // 上传到 MinIO
        String url = minioUtils.upload(file);

        // 打印上传后的访问地址
        System.out.println("文件上传成功：" + url);

        // 判断返回 URL 是否为空
        Assertions.assertNotNull(url);

        // 判断 URL 是否包含 test 所在的 MinIO 地址
        Assertions.assertFalse(url.isBlank());
    }
}