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
package com.apzda.cloud.oss.ali.backend;

import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.model.ObjectMetadata;
import com.apzda.cloud.oss.ali.file.AliOssFile;
import com.apzda.cloud.oss.backend.OssBackend;
import com.apzda.cloud.oss.config.BackendConfig;
import com.apzda.cloud.oss.file.IOssFile;
import com.apzda.cloud.oss.proto.FileInfo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
@Getter
public class AliOssBackend implements OssBackend {

    private OSSClient ossClient;

    private BackendConfig config;

    @Override
    public boolean init(BackendConfig config) {
        this.config = config;
        try {
            val endpoint = config.getEndpoint();
            val accessKeyId = config.getAccessKey();
            val accessKeySecret = config.getSecretKey();
            val conf = new ClientConfiguration();
            conf.setRequestTimeoutEnabled(true);
            conf.setRequestTimeout((int) config.getUploadTimeout().toMillis());
            conf.setConnectionTimeout((int) config.getConnectTimeout().toMillis());
            conf.setSocketTimeout((int) config.getReadTimeout().toMillis());

            this.ossClient = new OSSClient(endpoint, new DefaultCredentialProvider(accessKeyId, accessKeySecret), conf);

            return true;
        }
        catch (Exception e) {
            log.error("Cannot initialize AliOss Client: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public IOssFile getFile(String filePath) throws IOException {
        if (ossClient == null) {
            throw new IOException("AliOss Client is not initialized");
        }
        return new AliOssFile(filePath, this);
    }

    @Override
    public FileInfo uploadFile(File file, String path) throws IOException {
        return uploadFile(new FileInputStream(file), file.getName(), path);
    }

    @Override
    public FileInfo uploadFile(InputStream stream, String fileName, String path) throws IOException {
        if (ossClient == null) {
            throw new IOException("AliOss Client is not initialized");
        }
        try (val bs = new BufferedInputStream(stream)) {
            val fileId = generateFileId(bs);
            val filePath = generatePath(fileName, fileId, config.getPathPatten(), path);
            val meta = new ObjectMetadata();
            meta.addUserMetadata("fileid", fileId);
            meta.addUserMetadata("filename", fileName);
            meta.addUserMetadata("createtime", String.valueOf(System.currentTimeMillis()));
            val result = ossClient.putObject(config.getBucketName(), filePath.substring(1), bs, meta);
            if (result == null) {
                throw new IOException("Cannot upload file: response is null");
            }

            val ossFile = getFile(filePath);
            return ossFile.stat();
        }
        catch (FileAlreadyExistsException e) {
            val filePath = e.getFile();
            val ossFile = getFile(filePath);

            return ossFile.stat();
        }
    }

    @Override
    public boolean delete(String filePath) throws IOException {
        if (ossClient == null) {
            throw new IOException("AliOss Client is not initialized");
        }
        val result = ossClient.deleteObject(config.getBucketName(), filePath.substring(1));
        return result != null && result.getResponse().isSuccessful();
    }

}
