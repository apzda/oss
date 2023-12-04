package com.apzda.cloud.oss.config;

import com.apzda.cloud.oss.backend.OssBackend;
import com.apzda.cloud.oss.tx.backend.TxOssBackend;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(TxOssBackend.class)
@ConditionalOnProperty(value = "apzda.cloud.oss.backend", havingValue = "txoss")
class TxOssBackendConfiguration {

    @Bean("txossOssBackend")
    OssBackend txossOssBackend() {
        return new TxOssBackend();
    }

}
