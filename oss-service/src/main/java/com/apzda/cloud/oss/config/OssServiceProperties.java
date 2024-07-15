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

import com.apzda.cloud.gsvc.config.Props;
import com.apzda.cloud.oss.plugin.Plugin;
import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DataSizeUnit;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@ConfigurationProperties(prefix = "apzda.cloud.oss.server")
@Data
public class OssServiceProperties {

    @Getter(AccessLevel.PRIVATE)
    public static final List<String> DEFAULT_FILE_TYPES = List.of("png", "jpeg", "jpg", "zip", "txt", "rar", "7z",
            "xls", "xlsx", "doc", "docx", "xml", "json");

    @DataSizeUnit(DataUnit.MEGABYTES)
    private DataSize maxFileSize = DataSize.ofMegabytes(2);

    private String previewPath;

    private String downloadPath;

    private List<String> fileTypes = DEFAULT_FILE_TYPES;

    private List<PluginConfig> plugins = new ArrayList<>();

    public List<String> getFileTypes() {
        if (fileTypes == null) {
            fileTypes = DEFAULT_FILE_TYPES;
        }
        return fileTypes;
    }

    @Data
    @ToString(exclude = { "pluginClass" })
    public static class PluginConfig {

        private String id;

        private Class<? extends Plugin> pluginClass;

        private List<String> fileTypes;

        private final Map<String, String> props = new HashMap<>();

        @Setter(AccessLevel.PRIVATE)
        @Getter(AccessLevel.PRIVATE)
        private Plugin instance;

        @Setter(AccessLevel.PRIVATE)
        @Getter(AccessLevel.PRIVATE)
        private Props pluginProps;

        public Plugin instance() {
            return instance;
        }

        public void instance(Plugin instance) {
            this.instance = instance;
        }

        public synchronized Props props() {
            if (pluginProps == null) {
                pluginProps = new Props(props);
            }
            return pluginProps;
        }

    }

}
