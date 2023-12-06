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
package com.apzda.cloud.oss.minio.file;

import cn.hutool.core.io.FileUtil;
import com.apzda.cloud.oss.config.BackendConfig;
import com.apzda.cloud.oss.file.IOssFile;
import com.apzda.cloud.oss.minio.backend.MinioBackend;
import com.apzda.cloud.oss.proto.FileInfo;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.FileCopyUtils;

import java.io.*;
import java.net.URLConnection;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public class MinioFile implements IOssFile {

    private final String filePath;

    private final String objectName;

    private final BackendConfig config;

    private final MinioClient ossClient;

    private final MinioBackend backend;

    public MinioFile(String path, MinioBackend backend) throws IOException {
        if (path.startsWith("/")) {
            this.filePath = path;
            this.objectName = path.substring(1);
        }
        else {
            this.filePath = "/" + path;
            this.objectName = path;
        }
        this.backend = backend;
        this.config = backend.getConfig();
        this.ossClient = backend.getOssClient();
        if (this.ossClient == null) {
            throw new IOException("MinIO Client is not initialized");
        }
    }

    @Override
    public File getLocalFile() throws IOException {
        val tmpDir = config.getTmpDir();
        val tempFile = FileUtil.createTempFile(new File(tmpDir));
        FileCopyUtils.copy(getInputStream(), new BufferedOutputStream(new FileOutputStream(tempFile)));
        return tempFile;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        val bucketName = config.getBucketName();
        val builder = GetObjectArgs.builder();
        val arg = builder.bucket(bucketName).object(objectName).build();
        try {
            val ossObject = ossClient.getObject(arg);
            return new BufferedInputStream(ossObject);
        }
        catch (IOException ie) {
            throw ie;
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public FileInfo stat() throws FileNotFoundException {
        try {
            val argBuilder = StatObjectArgs.builder();
            val args = argBuilder.bucket(config.getBucketName()).object(objectName).build();
            val meta = ossClient.statObject(args);
            val userMeta = meta.userMetadata();
            val fileId = userMeta.getOrDefault("fileid", "");
            val filename = userMeta.getOrDefault("filename", FileUtil.getName(filePath));
            val builder = FileInfo.newBuilder();
            builder.setError(0);
            builder.setExist(true);
            builder.setPath(filePath);
            builder.setUrl(theUrl(filePath));
            builder.setLength(meta.size());
            builder.setFileId(fileId);
            builder.setFilename(filename);
            builder.setContentType(URLConnection.guessContentTypeFromName(filename));
            builder.setExt(FileUtil.extName(builder.getFilename()));
            builder.setCreateTime(Long.parseLong(userMeta.getOrDefault("createtime", "0")));
            builder.setBackend("minio");
            return builder.build();
        }
        catch (Exception e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public boolean delete() throws IOException {
        return backend.delete(filePath);
    }

    private String theUrl(String filePath) {
        val baseUrl = config.getBaseUrl();
        if (StringUtils.isBlank(baseUrl)) {
            val bucketName = config.getBucketName();
            val endpoint = config.getEndpoint();
            return endpoint + bucketName + filePath;
        }
        return baseUrl + filePath;
    }

}
