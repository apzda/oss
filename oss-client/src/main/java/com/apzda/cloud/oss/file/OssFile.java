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
package com.apzda.cloud.oss.file;

import cn.hutool.core.util.StrUtil;
import com.apzda.cloud.oss.backend.OssBackend;
import com.apzda.cloud.oss.config.OssContext;
import com.apzda.cloud.oss.proto.FileInfo;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class OssFile implements IOssFile {

    private final String filePath;

    private IOssFile ossFile;

    private FileInfo fileInfo;

    public OssFile(String filePath) {
        if (StringUtils.isBlank(filePath)) {
            throw new IllegalStateException("filePath cannot be blank");
        }

        if (!filePath.startsWith("/")) {
            this.filePath = "/" + filePath;
        }
        else {
            this.filePath = filePath;
        }

        try {
            OssBackend backend = OssContext.getOssBackend();
            ossFile = backend.getFile(this.filePath);
        }
        catch (IOException e) {
            log.warn("Failed to open file {}: {}", filePath, e.getMessage());
        }
    }

    @Nonnull
    public static Optional<OssBackend> getDefaultBackend() {
        try {
            val ossBackend = OssContext.getOssBackend();
            return Optional.of(ossBackend);
        }
        catch (Exception e) {
            log.warn("Failed to get default backend: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public File getLocalFile() throws IOException {
        if (ossFile == null) {
            throw new FileNotFoundException(StrUtil.format("{} (Not Found)", filePath));
        }
        return ossFile.getLocalFile();
    }

    public InputStream getInputStream() throws IOException {
        if (ossFile == null) {
            throw new FileNotFoundException(StrUtil.format("{} (Not Found)", filePath));
        }
        return ossFile.getInputStream();
    }

    public FileInfo stat() throws IOException {
        if (fileInfo != null) {
            return fileInfo;
        }
        if (ossFile == null) {
            throw new FileNotFoundException(StrUtil.format("{} (Not Found)", filePath));
        }
        synchronized (filePath) {
            if (fileInfo != null) {
                return fileInfo;
            }
            fileInfo = ossFile.stat();
        }
        return fileInfo;
    }

    @Override
    public boolean delete() throws IOException {
        if (ossFile == null) {
            throw new FileNotFoundException(StrUtil.format("{} (Not Found)", filePath));
        }
        return ossFile.delete();
    }

}
