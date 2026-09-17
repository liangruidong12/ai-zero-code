package com.wait.aizerocode;


import com.wait.aizerocode.utils.WebScreenshotUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SeleniumTest {

    @Test
    public void testScreenshot(){
        String s = WebScreenshotUtils.saveWebPageScreenshot("https://www.baidu.com");
        Assertions.assertNotNull(s);

    }


}
