package com.apzda.cloud.oss.config;

import com.apzda.cloud.oss.backend.OssBackend;
import com.apzda.cloud.oss.tx.backend.TxCosBackend;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(TxCosBackend.class)
@ConditionalOnProperty(value = "apzda.cloud.oss.backend", havingValue = "txcos")
class TxCosBackendConfiguration {

    @Bean("txcosOssBackend")
    OssBackend txcosOssBackend() {
        return new TxCosBackend();
    }

}
