/*
 * Copyright (C) 2023-2023 Fengz Ning (windywany@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.apzda.cloud.oss.config;

import com.apzda.cloud.oss.backend.OssBackend;
import com.apzda.cloud.oss.fs.backend.FsBackend;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Import({ OssClientHelper.class, AliOssBackendConfiguration.class, MinioBackendConfiguration.class,
        TxCosBackendConfiguration.class })
@AutoConfiguration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@EnableConfigurationProperties(OssConfigProperties.class)
@RequiredArgsConstructor
@Slf4j
public class OssClientAutoConfiguration {

    private static final BackendConfig DEFAULT_BACKEND_CONFIG = new BackendConfig();

    private final OssConfigProperties properties;

    private final ApplicationContext context;

    private volatile OssBackend ossBackend;

    OssBackend getBackend() {
        if (ossBackend != null) {
            return ossBackend;
        }
        synchronized (properties) {
            if (ossBackend != null) {
                return ossBackend;
            }

            val backend = StringUtils.defaultIfBlank(properties.getBackend(), "fs");
            val backends = properties.getBackends();
            val config = backends.getOrDefault(backend, DEFAULT_BACKEND_CONFIG);

            val clazz = config.getClazz();
            if (clazz == null) {
                try {
                    ossBackend = context.getBean(backend + "OssBackend", OssBackend.class);
                    log.debug("Found oss backend by name: {}OssBackend - {}", backend, ossBackend);
                }
                catch (Exception e) {
                    throw new NullPointerException("Clazz of '" + backend + "' is null");
                }
            }
            else {
                ossBackend = BeanUtils.instantiateClass(clazz);
            }
            config.setTmpDir(properties.getTmpDir());

            if (StringUtils.isBlank(config.getPathPatten())) {
                config.setPathPatten(properties.getPathPatten());
            }

            config.setBaseUrl(properties.getBaseUrl());

            if (!ossBackend.init(config)) {
                log.warn("Cannot initialize '{}' OssBackend: {}", backend, config);
            }

            log.trace("oss backend initialized: {}", config);
        }
        return ossBackend;
    }

    @Bean("fsOssBackend")
    @ConditionalOnProperty(value = "apzda.cloud.oss.backend", havingValue = "fs", matchIfMissing = true)
    OssBackend fsOssBackend() {
        return new FsBackend();
    }

}
