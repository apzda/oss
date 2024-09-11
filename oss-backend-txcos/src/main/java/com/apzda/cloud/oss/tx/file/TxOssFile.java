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
package com.apzda.cloud.oss.tx.file;

import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.SecureUtil;
import com.apzda.cloud.oss.backend.OssBackend;
import com.apzda.cloud.oss.config.BackendConfig;
import com.apzda.cloud.oss.file.IOssFile;
import com.apzda.cloud.oss.proto.FileInfo;
import com.apzda.cloud.oss.tx.backend.TxCosBackend;
import com.qcloud.cos.COSClient;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class TxOssFile implements IOssFile {

    private final String filePath;

    private final BackendConfig config;

    private final COSClient ossClient;

    private final String objectName;

    private final OssBackend backend;

    public TxOssFile(String path, TxCosBackend backend) throws IOException {
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
        this.ossClient = backend.getCosClient();
        if (this.ossClient == null) {
            throw new IOException("TxCos Client is not initialized");
        }
    }

    @Override
    public File getLocalFile() throws IOException {
        val tmpDir = config.getTmpDir();
        val stat = stat();
        val localFileName = tmpDir + SecureUtil.md5(stat.getFileId()) + "." + stat.getExt();
        var localFile = new File(localFileName);
        if (localFile.exists()) {
            return localFile;
        }
        synchronized (filePath) {
            if (localFile.exists()) {
                return localFile;
            }
            FileCopyUtils.copy(getInputStream(), new BufferedOutputStream(new FileOutputStream(localFile)));
            return localFile;
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        val bucketName = config.getBucketName();
        val ossObject = ossClient.getObject(bucketName, objectName);

        return new BufferedInputStream(ossObject.getObjectContent());
    }

    @Override
    public FileInfo stat() throws IOException {
        try {
            val meta = ossClient.getObjectMetadata(config.getBucketName(), objectName);
            val userMeta = meta.getUserMetadata();
            val filename = userMeta.getOrDefault("filename", FileUtil.getName(filePath));
            val builder = FileInfo.newBuilder();
            builder.setError(0);
            builder.setExist(true);
            builder.setPath(filePath);
            builder.setUrl(theUrl(filePath));
            builder.setLength(meta.getContentLength());
            builder.setFileId(meta.getContentMD5());
            builder.setFilename(filename);
            builder.setContentType(URLConnection.guessContentTypeFromName(filename));
            builder.setExt(FileUtil.extName(builder.getFilename()));
            builder.setCreateTime(Long.parseLong(userMeta.getOrDefault("createtime", "0")));
            builder.setBackend("txcos");
            return builder.build();
        }
        catch (Exception e) {
            throw new FileNotFoundException(filePath + ": " + e.getMessage());
        }
    }

    @Override
    public boolean delete() throws IOException {
        return backend.delete(filePath);
    }

    private String theUrl(String filePath) {
        val baseUrl = config.getBaseUrl();
        if (StringUtils.isBlank(baseUrl)) {
            val endpoint = config.getEndpoint();
            return "https://" + endpoint + filePath;
        }
        return baseUrl + filePath;
    }

}
