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

import com.apzda.cloud.oss.backend.OssBackend;
import com.apzda.cloud.oss.config.OssContext;
import com.apzda.cloud.oss.proto.FileInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class OssFile implements IOssFile {

    private final IOssFile ossFile;

    private FileInfo fileInfo;

    public OssFile(String filePath) throws IOException {
        if (StringUtils.isBlank(filePath)) {
            throw new IllegalStateException("filePath cannot be blank");
        }

        if (!filePath.startsWith("/")) {
            filePath = "/" + filePath;
        }

        OssBackend backend = OssContext.getOssBackend();

        ossFile = backend.getFile(filePath);
    }

    public File getLocalFile() throws IOException {
        return ossFile.getLocalFile();
    }

    public InputStream getInputStream() throws IOException {
        return ossFile.getInputStream();
    }

    public FileInfo stat() throws IOException {
        if (fileInfo != null) {
            return fileInfo;
        }
        synchronized (ossFile) {
            if (fileInfo != null) {
                return fileInfo;
            }
            fileInfo = ossFile.stat();
        }
        return fileInfo;
    }

    @Override
    public boolean delete() throws IOException {
        return ossFile.delete();
    }

}
