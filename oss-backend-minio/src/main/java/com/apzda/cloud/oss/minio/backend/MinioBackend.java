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
package com.apzda.cloud.oss.minio.backend;

import com.apzda.cloud.oss.backend.OssBackend;
import com.apzda.cloud.oss.config.BackendConfig;
import com.apzda.cloud.oss.file.IOssFile;
import com.apzda.cloud.oss.minio.file.MinioFile;
import com.apzda.cloud.oss.proto.FileInfo;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.HashMap;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
@Getter
public class MinioBackend implements OssBackend {

    private MinioClient ossClient;

    private BackendConfig config;

    private String bucketName;

    @Override
    public boolean init(BackendConfig config) {
        this.config = config;
        this.bucketName = config.getBucketName();
        try {
            val accessKey = config.getAccessKey();
            val secretKey = config.getSecretKey();

            val builder = MinioClient.builder().endpoint(config.getEndpoint()).credentials(accessKey, secretKey);

            val region = config.getRegion();
            if (StringUtils.isNotBlank(region)) {
                builder.region(region);
            }

            val httpClientBuilder = new OkHttpClient().newBuilder();
            httpClientBuilder.connectTimeout(config.getConnectTimeout());
            httpClientBuilder.readTimeout(config.getReadTimeout());
            httpClientBuilder.writeTimeout(config.getUploadTimeout());

            builder.httpClient(httpClientBuilder.build());

            ossClient = builder.build();

            return true;
        }
        catch (Exception e) {
            log.error("Cannot initialize MinIO Client: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public IOssFile getFile(String filePath) throws IOException {
        if (ossClient == null) {
            throw new IOException("MinIO Client is not initialized");
        }
        return new MinioFile(filePath, this);
    }

    @Override
    public FileInfo uploadFile(File file, String path) throws IOException {
        return uploadFile(new FileInputStream(file), file.getName(), path);
    }

    @Override
    public FileInfo uploadFile(InputStream stream, String fileName, String path) throws IOException {
        if (ossClient == null) {
            throw new IOException("MinIO Client is not initialized");
        }
        try (val bs = new BufferedInputStream(stream)) {
            val filePath = generatePath(fileName, config.getPathPatten(), path);
            val builder = PutObjectArgs.builder();

            val meta = new HashMap<String, String>();
            meta.put("filename", fileName);
            meta.put("createtime", String.valueOf(System.currentTimeMillis()));

            builder.userMetadata(meta);

            builder.bucket(bucketName).object(filePath.substring(1)).stream(bs, -1, 10485760);

            val result = ossClient.putObject(builder.build());
            if (result == null) {
                throw new IOException("Cannot upload file: response is null");
            }

            val ossFile = getFile(filePath);

            return ossFile.stat();
        }
        catch (IOException e) {
            throw e;
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean delete(String filePath) throws IOException {
        if (ossClient == null) {
            throw new IOException("MinIO Client is not initialized");
        }
        val builder = RemoveObjectArgs.builder();
        val args = builder.bucket(bucketName).object(filePath.substring(1)).build();
        try {
            ossClient.removeObject(args);
            return true;
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

}
