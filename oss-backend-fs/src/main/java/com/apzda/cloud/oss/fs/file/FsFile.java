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
package com.apzda.cloud.oss.fs.file;

import cn.hutool.core.io.FileUtil;
import com.apzda.cloud.oss.config.BackendConfig;
import com.apzda.cloud.oss.file.IOssFile;
import com.apzda.cloud.oss.proto.FileInfo;
import lombok.val;
import org.springframework.util.DigestUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public class FsFile implements IOssFile {

    private final String filePath;

    private final File file;

    protected final BackendConfig config;

    protected final String baseUrl;

    protected final String rootDir;

    public FsFile(String filePath, BackendConfig config) {
        if (filePath.startsWith("/")) {
            this.filePath = filePath;
        }
        else {
            this.filePath = "/" + filePath;
        }
        this.config = config;
        this.baseUrl = config.getBaseUrl();
        this.rootDir = config.getRootDir();
        this.file = new File(rootDir + filePath);
    }

    @Override
    public File getLocalFile() throws IOException {
        return file;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new BufferedInputStream(new FileInputStream(file));
    }

    @Override
    public FileInfo stat() throws IOException {
        val builder = FileInfo.newBuilder();
        builder.setExist(file.exists());
        builder.setBackend("fs");
        if (file.exists()) {
            builder.setError(0);
            builder.setPath(filePath);
            builder.setUrl(baseUrl + filePath);
            builder.setLength(file.length());
            builder.setExt(FileUtil.extName(file));
            builder.setFilename(file.getName());
            builder.setFileId(DigestUtils.md5DigestAsHex(new FileInputStream(file)));
            try {
                FileTime creationTime = (FileTime) Files.getAttribute(file.toPath(), "creationTime");
                builder.setCreateTime(creationTime.toMillis());
            }
            catch (IOException e) {
                builder.setCreateTime(0);
            }
            return builder.build();
        }
        else {
            throw new FileNotFoundException("File not found: " + filePath);
        }
    }

    @Override
    public boolean delete() throws IOException {
        return file.delete();
    }

}
