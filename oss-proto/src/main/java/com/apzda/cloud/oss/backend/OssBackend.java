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
package com.apzda.cloud.oss.backend;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import com.apzda.cloud.oss.config.BackendConfig;
import com.apzda.cloud.oss.file.IOssFile;
import com.apzda.cloud.oss.proto.FileInfo;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.DigestUtils;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.util.Date;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public interface OssBackend {

    Logger logger = LoggerFactory.getLogger(OssBackend.class);

    default boolean init(BackendConfig config) {
        return true;
    }

    IOssFile getFile(String filePath) throws IOException;

    FileInfo uploadFile(File file, String path) throws IOException;

    FileInfo uploadFile(InputStream stream, String fileName, String path) throws IOException;

    default FileInfo uploadFile(InputStream stream, String fileName) throws IOException {
        return uploadFile(stream, fileName, null);
    }

    boolean delete(String filePath) throws IOException;

    default FileInfo uploadFile(File file) throws IOException {
        return uploadFile(file, null);
    }

    default boolean close() {
        return true;
    }

    default String generatePath(File file, String pathPatten, String path) throws FileAlreadyExistsException {

        try (var fin = new FileInputStream(file)) {
            val fileId = DigestUtils.md5DigestAsHex(fin);
            return generatePath(file.getName(), fileId, pathPatten, path);
        }
        catch (FileAlreadyExistsException fe) {
            throw fe;
        }
        catch (IOException e) {
            logger.warn("Cannot md5 the file: {} - {}", file.getAbsoluteFile(), e.getMessage());
            return "/" + StringUtils.strip(path, "/") + "/" + file.getName();
        }
    }

    default String generatePath(String fileName, String fileId, String pathPatten, String path)
            throws FileAlreadyExistsException {
        if (StringUtils.isBlank(path)) {
            path = DateUtil.format(new Date(), StringUtils.defaultIfBlank(pathPatten, "yyyy/MM/dd"));
        }
        try {
            val fName = fileId.substring(0, 10);
            val extName = "." + FileUtil.extName(fileName);
            val destFile = "/" + StringUtils.strip(path, "/") + "/" + fName;
            int i = 1;
            var finalFileName = destFile + extName;
            var ossFile = getFile(finalFileName);
            do {
                try {
                    val stat = ossFile.stat();

                    if (stat == null || !stat.getExist()) {
                        break;
                    }
                    if (fileId.equals(stat.getFileId())) {
                        throw new FileAlreadyExistsException(finalFileName);
                    }
                    finalFileName = destFile + "-" + (i++) + extName;
                    ossFile = getFile(finalFileName);
                }
                catch (FileNotFoundException e) {
                    break;
                }
            }
            while (true);

            return finalFileName;
        }
        catch (FileAlreadyExistsException fe) {
            throw fe;
        }
        catch (IOException e) {
            return "/" + StringUtils.strip(path, "/") + "/" + fileName;
        }
    }

}
