package com.apzda.cloud.oss.config;

import com.apzda.cloud.oss.ali.backend.AliOssBackend;
import com.apzda.cloud.oss.backend.OssBackend;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(AliOssBackend.class)
@ConditionalOnProperty(value = "apzda.cloud.oss.backend", havingValue = "alioss")
class AliOssBackendConfiguration {

    @Bean("aliossOssBackend")
    OssBackend aliossOssBackend() {
        return new AliOssBackend();
    }

}
