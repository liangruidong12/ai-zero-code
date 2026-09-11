package com.wait.aizerocode.config;

import com.wait.aizerocode.constant.AppConstant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 静态资源配置
 * 将 AI 生成的网站文件目录（tmp/code_output）暴露为 /api/static/** 供前端 iframe 预览
 * 目录名规则：{codeGenType}_{appId}，如 html_123、multi_file_456、vue_project_789
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    private static final String URL_PREFIX = "/api/static/";

    /**
     * 生成文件根目录（统一为正斜杠，兼容 Windows 下的 file: 协议路径）
     */
    private final String outputRootDir = AppConstant.CODE_OUTPUT_ROOT_DIR.replace(File.separatorChar, '/');

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = outputRootDir.endsWith("/") ? outputRootDir : outputRootDir + "/";
        registry.addResourceHandler("/api/static/**")
                .addResourceLocations("file:" + location)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    public Resource resolveResource(HttpServletRequest request, String requestPath,
                                                    List<? extends Resource> locations,
                                                    ResourceResolverChain chain) {
                        // 先按普通文件解析（html / css / js / 图片等）
                        Resource resource = super.resolveResource(request, requestPath, locations, chain);
                        if (resource != null) {
                            return resource;
                        }
                        // 请求的是目录（无论是否带结尾斜杠）时，返回该目录下的 index.html
                        String dirPath = requestPath.endsWith("/") ? requestPath : requestPath + "/";
                        return super.resolveResource(request, dirPath + "index.html", locations, chain);
                    }
                });
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 访问目录但缺少结尾斜杠时（如 /api/static/multi_file_123），302 补全斜杠。
        // 否则多文件站点 index.html 中的 css/js 相对路径会按上一级目录解析，导致资源 404。
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                    throws IOException {
                String uri = request.getRequestURI();
                if (!uri.startsWith(URL_PREFIX) || uri.endsWith("/")) {
                    return true;
                }
                String relativePath = uri.substring(URL_PREFIX.length());
                File target = new File(outputRootDir, relativePath);
                // canonical 校验，防止路径穿越访问根目录之外
                File root = new File(outputRootDir);
                if (target.isDirectory() && target.getCanonicalPath().startsWith(root.getCanonicalPath())) {
                    response.sendRedirect(uri + "/");
                    return false;
                }
                return true;
            }
        }).addPathPatterns("/api/static/**");
    }
}
