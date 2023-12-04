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
package com.apzda.cloud.oss.fs.backend;

import com.apzda.cloud.oss.backend.OssBackend;
import com.apzda.cloud.oss.config.BackendConfig;
import com.apzda.cloud.oss.file.IOssFile;
import com.apzda.cloud.oss.fs.file.FsFile;
import com.apzda.cloud.oss.proto.FileInfo;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class FsBackend implements OssBackend {

    private BackendConfig config;

    private String rootDir;

    @Override
    public boolean init(BackendConfig config) {
        this.config = config;
        this.rootDir = config.getRootDir();
        val file = new File(rootDir);
        val absolutePath = file.getAbsolutePath();
        if (!file.exists()) {
            if (!file.mkdirs()) {
                throw new IllegalStateException("Cannot create directory: " + absolutePath);
            }
        }
        return true;
    }

    @Override
    public IOssFile getFile(String filePath) throws IOException {
        if (filePath.startsWith("/")) {
            return new FsFile(filePath, config);
        }
        return new FsFile("/" + filePath, config);
    }

    @Override
    public FileInfo uploadFile(File file, String path) throws IOException {
        try {
            val destFileName = generatePath(file, config.getPathPatten(), path);
            val destFile = new File(rootDir + destFileName);
            val absolutePath = destFile.getParentFile().getAbsolutePath();
            val parentDir = new File(absolutePath);
            try {
                if (!parentDir.exists() && !parentDir.mkdirs()) {
                    throw new IOException("Cannot create directory: " + absolutePath);
                }
                FileCopyUtils.copy(file, destFile);
                val ossFile = getFile(destFileName);
                val stat = ossFile.stat();
                return FileInfo.newBuilder(stat).setFilename(file.getName()).build();
            }
            catch (Exception e) {
                log.error("文件上传失败: {} - {}", file, path, e);
                val builder = FileInfo.newBuilder();
                builder.setBackend("fs");
                builder.setError(1);
                builder.setFilename(file.getName());
                builder.setMessage(e.getMessage());
                return builder.build();
            }
        }
        catch (FileAlreadyExistsException e) {
            log.debug("file already exists: {}", file.getAbsolutePath());
            val ossFile = getFile(e.getFile());
            val stat = ossFile.stat();
            return FileInfo.newBuilder(stat).setFilename(file.getName()).build();
        }
    }

    @Override
    public FileInfo uploadFile(InputStream stream, String fileName, String path) throws IOException {
        return null;
    }

    @Override
    public boolean delete(String filePath) throws IOException {
        val ossFile = getFile(filePath);
        return ossFile.delete();
    }

}
