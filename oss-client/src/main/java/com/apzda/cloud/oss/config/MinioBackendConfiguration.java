package com.apzda.cloud.oss.config;

import com.apzda.cloud.oss.backend.OssBackend;
import com.apzda.cloud.oss.minio.backend.MinioBackend;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(MinioBackend.class)
@ConditionalOnProperty(value = "apzda.cloud.oss.backend", havingValue = "minio")
class MinioBackendConfiguration {

    @Bean("minioOssBackend")
    OssBackend minioOssBackend() {
        return new MinioBackend();
    }

}
