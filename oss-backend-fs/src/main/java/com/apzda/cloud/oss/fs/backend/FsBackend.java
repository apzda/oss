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

import java.io.*;
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
        return uploadFile(new FileInputStream(file), file.getName(), path);
    }

    @Override
    public FileInfo uploadFile(InputStream stream, String fileName, String path) throws IOException {
        try (val bs = new BufferedInputStream(stream)) {
            val fileId = generateFileId(bs);
            val filePath = generatePath(fileName, fileId, config.getPathPatten(), path);
            val destFile = new File(rootDir + filePath);
            val absolutePath = destFile.getParentFile().getAbsolutePath();
            val parentDir = new File(absolutePath);
            try {
                if (!parentDir.exists() && !parentDir.mkdirs()) {
                    throw new IOException("Cannot create directory: " + absolutePath);
                }
                FileCopyUtils.copy(bs, new FileOutputStream(destFile));
                val ossFile = getFile(filePath);
                val stat = ossFile.stat();
                return FileInfo.newBuilder(stat).setFilename(fileName).build();
            }
            catch (IOException ie) {
                throw ie;
            }
            catch (Exception e) {
                throw new IOException(e);
            }
        }
        catch (FileAlreadyExistsException e) {
            val ossFile = getFile(e.getFile());
            val stat = ossFile.stat();
            return FileInfo.newBuilder(stat).setFilename(fileName).build();
        }
    }

    @Override
    public boolean delete(String filePath) throws IOException {
        val ossFile = getFile(filePath);
        return ossFile.delete();
    }

}
