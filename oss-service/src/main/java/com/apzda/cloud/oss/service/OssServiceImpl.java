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
package com.apzda.cloud.oss.service;

import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.apzda.cloud.oss.config.OssClientHelper;
import com.apzda.cloud.oss.config.OssConfigProperties;
import com.apzda.cloud.oss.proto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Service
@Slf4j
@RequiredArgsConstructor
public class OssServiceImpl implements OssService {

    private final OssConfigProperties properties;

    @Override
    public UploadRes upload(UploadReq request) {
        val ossBackend = OssClientHelper.getOssBackend();
        val builder = UploadRes.newBuilder();
        val fileCount = request.getFilesCount();
        val path = request.getPath();
        log.debug("Start dealing file upload: fileCount = {}, path = {}, backend = {}", fileCount, path,
                properties.getBackend());
        if (fileCount > 0) {
            val futures = new ArrayList<CompletableFuture<FileInfo>>();
            for (int i = 0; i < fileCount; i++) {
                GsvcExt.UploadFile file = request.getFiles(i);
                val index = i;
                CompletableFuture<FileInfo> uploaded = CompletableFuture.supplyAsync(() -> {
                    FileInfo.Builder fileInfo;
                    val filename = file.getFilename();
                    try {
                        String tmpFilePath = file.getFile();
                        val error = file.getError();
                        val result = ossBackend.uploadFile(new FileInputStream(tmpFilePath), filename, path);
                        fileInfo = FileInfo.newBuilder(result);
                        log.debug("Upload success: fileName = {}, path = {}, size = {}", filename, fileInfo.getPath(),
                                DataSize.ofBytes(fileInfo.getLength()));
                    }
                    catch (Exception e) {
                        fileInfo = FileInfo.newBuilder();
                        fileInfo.setError(1);
                        fileInfo.setMessage(e.getMessage());
                        log.error("Upload fail: fileName = {}, error = {}", filename, e.getMessage());
                    }
                    fileInfo.setFilename(filename);
                    fileInfo.setIndex(index);
                    return fileInfo.build();
                });
                futures.add(uploaded);
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[fileCount])).join();
            for (CompletableFuture<FileInfo> future : futures) {
                try {
                    builder.addFiles(future.get());
                }
                catch (Exception e) {
                    log.warn("error !!!", e);
                }
            }
        }
        builder.setErrCode(0);
        return builder.build();
    }

    @Override
    public ChunkUploadRes chunkUpload(ChunkUploadReq request) {

        return null;
    }

    @Override
    public ChunkUploadRes checkChunk(CheckChunkReq request) {
        return null;
    }

}
