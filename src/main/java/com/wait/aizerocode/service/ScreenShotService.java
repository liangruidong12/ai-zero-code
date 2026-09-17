package com.wait.aizerocode.service;

import java.io.InputStream;

public interface ScreenShotService {

    String generateScreenShotAndUpload(String webUrl);

    void generateScreenShotAsync(Long appId, String appUrl);


}
