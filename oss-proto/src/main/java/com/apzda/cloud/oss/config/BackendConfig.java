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

import cn.hutool.core.io.FileUtil;
import com.apzda.cloud.oss.backend.OssBackend;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/

@Data
public class BackendConfig {

    private String rootDir = "uploads";

    private Class<? extends OssBackend> clazz;

    private String accessKey;

    private String accessToken;

    private String endpoint;

    private String bucketName;

    private String region;

    private String tmpDir;

    private String pathPatten;

    private String baseUrl;

    @DurationUnit(ChronoUnit.SECONDS)
    private Duration readTimeout = Duration.ofSeconds(30);

    @DurationUnit(ChronoUnit.SECONDS)
    private Duration uploadTimeout = Duration.ofSeconds(300);

    @DurationUnit(ChronoUnit.SECONDS)
    private Duration connectTimeout = Duration.ofSeconds(5);

    private final Map<String, String> props = new HashMap<>();

    public String getRootDir() {
        return StringUtils.stripEnd(StringUtils.defaultIfBlank(rootDir, "uploads"), "/");
    }

    public String getTmpDir() {
        return StringUtils.defaultIfBlank(tmpDir, FileUtil.getTmpDirPath());
    }

    public String getBaseUrl() {
        if (StringUtils.isBlank(baseUrl)) {
            return "";
        }
        else {
            return StringUtils.stripEnd(baseUrl, "/");
        }
    }

}
