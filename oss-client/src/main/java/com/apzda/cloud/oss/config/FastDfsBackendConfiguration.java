package com.apzda.cloud.oss.config;

import com.apzda.cloud.oss.backend.OssBackend;
import com.apzda.cloud.oss.fastdfs.backend.FastDfsBackend;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(FastDfsBackend.class)
@ConditionalOnProperty(value = "apzda.cloud.oss.backend", havingValue = "fastdfs")
class FastDfsBackendConfiguration {

    @Bean("fastdfsOssBackend")
    OssBackend fastdfsOssBackend() {
        return new FastDfsBackend();
    }

}
