package com.example.storageservicemodule;

import com.example.storageservicemodule.Configuration.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(StorageProperties.class)
public class StorageServiceModuleApplication {

    public static void main(String[] args) {
        SpringApplication.run(StorageServiceModuleApplication.class, args);
    }


    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/files/**")
                        .addResourceLocations("file:uploads/");
            }
        };
    }
}
