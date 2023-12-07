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
package com.apzda.cloud.oss.tx.backend;

import com.apzda.cloud.oss.backend.OssBackend;
import com.apzda.cloud.oss.config.BackendConfig;
import com.apzda.cloud.oss.file.IOssFile;
import com.apzda.cloud.oss.proto.FileInfo;
import com.apzda.cloud.oss.tx.file.TxOssFile;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.region.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

import java.io.*;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
@Getter
public class TxCosBackend implements OssBackend {

    private COSClient cosClient;

    private BackendConfig config;

    private String bucketName;

    @Override
    public boolean init(BackendConfig config) {
        bucketName = config.getBucketName();
        this.config = config;
        try {
            val secretId = config.getAccessKey();
            val secretKey = config.getSecretKey();

            val cred = new BasicCOSCredentials(secretId, secretKey);
            val region = config.getRegion();

            ClientConfig clientConfig;
            if (StringUtils.isNotBlank(region)) {
                clientConfig = new ClientConfig(new Region(region));
            }
            else {
                clientConfig = new ClientConfig();
            }

            clientConfig.setRequestTimeOutEnable(true);
            clientConfig.setRequestTimeout((int) config.getUploadTimeout().toMillis());
            clientConfig.setConnectionTimeout((int) config.getConnectTimeout().toMillis());
            clientConfig.setSocketTimeout((int) config.getReadTimeout().toMillis());
            clientConfig.setHttpProtocol(HttpProtocol.https);
            cosClient = new COSClient(cred, clientConfig);
            return true;
        }
        catch (Exception e) {
            log.error("Cannot initialize txCos Client: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public IOssFile getFile(String filePath) throws IOException {
        if (cosClient == null) {
            throw new IOException("TxCos Client is not initialized");
        }
        return new TxOssFile(filePath, this);
    }

    @Override
    public FileInfo uploadFile(File file, String path) throws IOException {
        return uploadFile(new FileInputStream(file), file.getName(), path);
    }

    @Override
    public FileInfo uploadFile(InputStream stream, String fileName, String path) throws IOException {
        if (cosClient == null) {
            throw new IOException("TxCos Client is not initialized");
        }

        try (val bs = new BufferedInputStream(stream)) {
            val filePath = generatePath(fileName, config.getPathPatten(), path);

            val meta = new ObjectMetadata();
            meta.addUserMetadata("filename", fileName);
            meta.addUserMetadata("createtime", String.valueOf(System.currentTimeMillis()));

            val result = cosClient.putObject(bucketName, filePath.substring(1), bs, meta);

            if (result == null) {
                throw new IOException("Cannot upload file: response is null");
            }

            val ossFile = getFile(filePath);

            return ossFile.stat();
        }
    }

    @Override
    public boolean delete(String filePath) throws IOException {
        if (cosClient == null) {
            throw new IOException("TxCos Client is not initialized");
        }
        cosClient.deleteObject(bucketName, filePath.substring(1));
        return true;
    }

}
