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
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
@ConfigurationProperties(prefix = "apzda.cloud.oss")
@Data
public class OssConfigProperties {

    /**
     * 存储后端
     */
    private String backend = "fs";

    /**
     * 访问URL
     */
    private String baseUrl;

    /**
     * 临时文件存放目录.
     */
    private String tmpDir;

    /**
     * 临时文件过期时间.
     */
    @DurationUnit(ChronoUnit.DAYS)
    private Duration expire = Duration.ofDays(7);

    /**
     * 基于日期的文件路径规则
     */
    private String pathPatten = "yyyy/MM/dd";

    private final Map<String, BackendConfig> backends = new HashMap<>();

    public String getTmpDir() {
        return StringUtils.defaultIfBlank(tmpDir, FileUtil.getTmpDirPath());
    }

    public String getBaseUrl() {
        if (StringUtils.isBlank(baseUrl)) {
            return "";
        }
        else {
            baseUrl = StringUtils.strip(baseUrl);
            if (baseUrl.equals("/")) {
                return baseUrl;
            }
            return StringUtils.stripEnd(baseUrl, "/");
        }
    }

    public String getPathPatten() {
        return StringUtils.defaultIfBlank(pathPatten, "yyyy/MM/dd");
    }

}
