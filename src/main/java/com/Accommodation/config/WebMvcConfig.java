package com.Accommodation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${uploadPath}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        String fullPath =
                System.getProperty("user.dir")
                        + "/"
                        + uploadPath
                        + "/";

        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:///" + fullPath.replace("\\", "/"));
    }
}