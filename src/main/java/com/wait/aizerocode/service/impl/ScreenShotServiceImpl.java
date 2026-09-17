package com.wait.aizerocode.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.wait.aizerocode.exception.ErrorCode;
import com.wait.aizerocode.exception.ThrowUtils;
import com.wait.aizerocode.model.entity.App;
import com.wait.aizerocode.service.AppService;
import com.wait.aizerocode.service.ScreenShotService;
import com.wait.aizerocode.utils.MinioUtils;
import com.wait.aizerocode.utils.WebScreenshotUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;

@Slf4j
@Service

public class ScreenShotServiceImpl implements ScreenShotService {

    @Resource
    private MinioUtils minioUtils;

    @Resource
    @Lazy
    private AppService appService;


    @Override
    public void generateScreenShotAsync(Long appId, String appUrl) {
        Thread.startVirtualThread( ()-> {
            String url = generateScreenShotAndUpload(appUrl);

            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setCover(url);
            boolean update =appService.updateById(updateApp);
            ThrowUtils.throwIf(!update,ErrorCode.SUCCESS,"跟新应用字段保存失败");
                }
        );
    }

    @Override
    public String generateScreenShotAndUpload(String webUrl) {

        // 1. 参数校验
        ThrowUtils.throwIf(
                StrUtil.isBlank(webUrl),
                ErrorCode.PARAMS_ERROR,
                "截图地址为空"
        );

        String filePath = null;

        try {

            // 2. 调用截图工具生成本地截图
            filePath = WebScreenshotUtils.saveWebPageScreenshot(webUrl);

            ThrowUtils.throwIf(
                    StrUtil.isBlank(filePath),
                    ErrorCode.SYSTEM_ERROR,
                    "网页截图失败"
            );

            File screenshotFile = new File(filePath);

            ThrowUtils.throwIf(
                    !screenshotFile.exists(),
                    ErrorCode.SYSTEM_ERROR,
                    "截图文件不存在"
            );

            // 3. 构造 MinIO 中的文件路径
            // screenshot/2026/09/17/xxx.png

            LocalDate now = LocalDate.now();

            String objectName = String.format(
                    "screenshot/%d/%02d/%02d/%s",
                    now.getYear(),
                    now.getMonthValue(),
                    now.getDayOfMonth(),
                    screenshotFile.getName()
            );

            log.info(
                    "准备上传截图到 MinIO，localPath={}, objectName={}",
                    filePath,
                    objectName
            );

            // 4. 上传 MinIO
            String fileUrl = minioUtils.uploadFile(
                    filePath,
                    objectName
            );

            ThrowUtils.throwIf(
                    StrUtil.isBlank(fileUrl),
                    ErrorCode.SYSTEM_ERROR,
                    "截图上传 MinIO 失败"
            );

            log.info(
                    "网页截图上传成功，webUrl={}, fileUrl={}",
                    webUrl,
                    fileUrl
            );

            // 5. 返回 MinIO URL
            return fileUrl;

        } finally {

            // 6. 无论上传成功还是失败，都删除本地临时截图
            if (StrUtil.isNotBlank(filePath)) {

                try {

                    File file = new File(filePath);

                    if (file.exists()) {

                        FileUtil.del(file);

                        log.info(
                                "本地截图删除成功：{}",
                                filePath
                        );
                    }

                } catch (Exception e) {

                    // 删除本地临时文件失败，不影响主业务
                    log.warn(
                            "删除本地截图失败：{}",
                            filePath,
                            e
                    );
                }
            }
        }
    }
}