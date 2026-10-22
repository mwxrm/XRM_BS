package com.xrm.xrm_bs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${upload.path:uploads/images/}")
    private String uploadPath;

    @Value("${upload.url-prefix:/images/}")
    private String urlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path projectDir = Paths.get(System.getProperty("user.dir"));
        Path uploadDir = projectDir.resolve(uploadPath).normalize().toAbsolutePath();
        String location = "file:" + uploadDir.toString() + File.separator;
        registry.addResourceHandler(urlPrefix + "**")
                .addResourceLocations(location);
    }
}