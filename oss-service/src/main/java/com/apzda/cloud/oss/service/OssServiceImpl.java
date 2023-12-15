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
import com.apzda.cloud.oss.backend.OssBackend;
import com.apzda.cloud.oss.cache.FileInfoCache;
import com.apzda.cloud.oss.config.OssClientHelper;
import com.apzda.cloud.oss.config.OssConfigProperties;
import com.apzda.cloud.oss.config.OssServiceProperties;
import com.apzda.cloud.oss.exception.ChunkNotFoundException;
import com.apzda.cloud.oss.exception.FileExtNameNotAllowedException;
import com.apzda.cloud.oss.exception.FileSizeNotAllowedException;
import com.apzda.cloud.oss.proto.*;
import com.apzda.cloud.oss.resumable.ResumableInfo;
import com.apzda.cloud.oss.resumable.ResumableStorage;
import com.google.common.base.Splitter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Service
@Slf4j
@RequiredArgsConstructor
public class OssServiceImpl implements OssService, InitializingBean {

    private final OssConfigProperties properties;

    private final OssServiceProperties serviceProperties;

    private final FileInfoCache fileInfoCache;

    private List<OssServiceProperties.PluginConfig> plugins;

    @Override
    public void afterPropertiesSet() throws Exception {
        plugins = serviceProperties.getPlugins();
    }

    @Override
    public UploadRes upload(UploadReq request) {
        if (request.hasFile()) {
            val newReq = UploadReq.newBuilder(request);
            newReq.addFiles(request.getFile());
            request = newReq.build();
        }
        val ossBackend = OssClientHelper.getOssBackend();
        val builder = UploadRes.newBuilder();
        val fileCount = request.getFilesCount();
        val path = request.getPath();
        log.debug("Start dealing file upload: fileCount = {}, path = {}, backend = {}", fileCount, path,
                properties.getBackend());
        if (fileCount > 0) {
            val finalReq = request;
            val futures = new ArrayList<CompletableFuture<FileInfo>>();
            val disables = Splitter.on(",").omitEmptyStrings().trimResults().splitToList(request.getDisables());
            for (int i = 0; i < fileCount; i++) {
                val index = i;
                CompletableFuture<FileInfo> uploaded = CompletableFuture.supplyAsync(() -> {
                    GsvcExt.UploadFile file = finalReq.getFiles(index);
                    FileInfo.Builder fileInfo;
                    val filename = file.getFilename();
                    try {
                        checkFileValid(file);
                        file = applyPlugins(file, path, ossBackend, disables);
                        val tmpFilePath = file.getFile();
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
                    finally {
                        val f = new File(file.getFile());
                        if (f.exists()) {
                            val deleted = f.delete();
                            log.debug("Delete the original file: {} - {}", deleted, f);
                        }
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
        if (!request.hasFile()) {
            return chunkCheck(request);
        }

        val chunkNumber = request.getChunkNumber();
        val resumableStorage = ResumableStorage.getInstance();
        val info = resumableStorage.getResumableInfo(request, properties.getTmpDir());
        val file = request.getFile();
        val path = request.getPath();
        if (log.isDebugEnabled()) {
            log.debug("Start dealing file chunk-upload: ResumableInfo = {}, backend = {}", info,
                    properties.getBackend());
        }

        if (info.resumableTotalSize > serviceProperties.getMaxFileSize().toBytes()) {
            log.warn("The size of {} is larger than {}", request.getFileName(), serviceProperties.getMaxFileSize());
            throw new FileSizeNotAllowedException("The size is larger than " + serviceProperties.getMaxFileSize());
        }

        checkFileValid(file);
        try (val is = new FileInputStream(file.getFile());
                val raf = new RandomAccessFile(info.resumableFilePath, "rw")) {
            // Seek to position
            raf.seek((chunkNumber - 1) * (long) info.resumableChunkSize);
            long readed = 0;
            long contentLength = file.getSize();
            byte[] bytes = new byte[1024 * 100];
            while (readed < contentLength) {
                int r = is.read(bytes);
                if (r < 0) {
                    break;
                }
                raf.write(bytes, 0, r);
                readed += r;
            }
            raf.close();

            info.uploadedChunks.add(new ResumableInfo.ResumableChunkNumber(chunkNumber));
            val builder = ChunkUploadRes.newBuilder();
            builder.setChunkNumber(chunkNumber);
            builder.setErrCode(0);
            val fInfo = FileInfo.newBuilder();
            fInfo.setFileId(info.resumableIdentifier);
            fInfo.setError(0);
            fInfo.setBackend(properties.getBackend());
            fInfo.setLength(contentLength);
            fInfo.setExt(file.getExt());
            fInfo.setFilename(request.getFileName());
            fInfo.setContentType(file.getContentType());
            fInfo.setMessage(file.getError());
            builder.setFile(fInfo.build());

            if (info.checkIfUploadFinished()) {
                // Check if all chunks uploaded, and change filename
                ResumableStorage.getInstance().remove(info);

                CompletableFuture.runAsync(() -> {
                    if (log.isDebugEnabled()) {
                        log.debug("All chunks are uploaded, now save it to backend[{}]: {}", properties.getBackend(),
                                info);
                    }

                    try {
                        val ossBackend = OssClientHelper.getOssBackend();
                        val disables = Splitter.on(",")
                            .omitEmptyStrings()
                            .trimResults()
                            .splitToList(request.getDisables());
                        val fBuilder = GsvcExt.UploadFile.newBuilder(file);
                        fBuilder.setFile(info.resumableFilePath);
                        fBuilder.setSize(info.resumableTotalSize);
                        val uploadFile = applyPlugins(fBuilder.build(), path, ossBackend, disables);
                        info.resumableFilePath = uploadFile.getFile();
                        try (val fileStream = new FileInputStream(info.resumableFilePath)) {
                            val fileInfo = ossBackend.uploadFile(fileStream, info.resumableFilename, path);
                            fileInfoCache.setFileInfo(info.resumableIdentifier, fileInfo);
                            if (log.isDebugEnabled()) {
                                log.debug("File saved to backend[{}]: {}", properties.getBackend(), info);
                            }
                        }
                    }
                    catch (Exception e) {
                        log.error("Cannot save file to backend: {} - {}", info, e.getMessage());
                        val fileInfo = FileInfo.newBuilder();
                        fileInfo.setError(1);
                        fileInfo.setMessage(e.getMessage());
                        fileInfoCache.setFileInfo(info.resumableIdentifier, fileInfo.build());
                    }
                    finally {
                        val f = new File(info.resumableFilePath);
                        if (f.exists()) {
                            val deleted = f.delete();
                            log.debug("Delete the original file: {} - {}", deleted, f);
                        }
                    }
                });
            }
            return builder.build();
        }
        catch (FileNotFoundException fne) {
            log.error("File not found: {} - {}", info, fne.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fne.getMessage());
        }
        catch (IOException e) {
            log.error("File cannot upload: {} - {}", info, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    public ChunkUploadRes chunkCheck(ChunkUploadReq request) {
        int chunkNumber = request.getChunkNumber();

        val resumableStorage = ResumableStorage.getInstance();
        val info = resumableStorage.getResumableInfo(request, properties.getTmpDir());

        if (info.uploadedChunks.contains(new ResumableInfo.ResumableChunkNumber(chunkNumber))) {
            val builder = ChunkUploadRes.newBuilder();
            builder.setErrCode(0);
            builder.setChunkNumber(chunkNumber);
            return builder.build();
        }
        else {
            throw new ChunkNotFoundException();
        }
    }

    @Override
    public ChunkUploadRes query(Query request) {
        val fileId = request.getFileId();
        val fileInfo = fileInfoCache.getFileInfo(fileId);
        val builder = ChunkUploadRes.newBuilder();
        builder.setErrCode(0);
        if (fileInfo != null) {
            builder.setFile(fileInfo);
        }
        return builder.build();
    }

    private void checkFileValid(GsvcExt.UploadFile file) {
        try {
            val error = file.getError();
            if (StringUtils.isNotBlank(error)) {
                throw new IllegalStateException(error);
            }
            val size = file.getSize();
            if (size == 0) {
                log.warn("The size of {} is 0", file.getFilename());
                throw new FileSizeNotAllowedException("The size is 0");
            }
            if (size > serviceProperties.getMaxFileSize().toBytes()) {
                log.warn("The size of {} is larger than {}", file.getFilename(), serviceProperties.getMaxFileSize());
                throw new FileSizeNotAllowedException("The size is larger than " + serviceProperties.getMaxFileSize());
            }
            val fileTypes = serviceProperties.getFileTypes();
            val ext = file.getExt();
            if (StringUtils.isBlank(ext) || !fileTypes.contains(ext.toLowerCase())) {
                log.warn("The extension of {} is now allowed: {}", file.getFilename(), ext);
                throw new FileExtNameNotAllowedException("The extension is now allowed: " + ext);
            }
        }
        catch (Exception e) {
            val f = new File(file.getFile());
            if (f.exists()) {
                val deleted = f.delete();
                log.debug("Delete the original file: {} - {}", deleted, f);
            }
            throw e;
        }
    }

    private GsvcExt.UploadFile applyPlugins(final GsvcExt.UploadFile file, final String path,
            final OssBackend ossBackend, List<String> disabledPlugins) throws Exception {

        val ext = file.getExt();
        GsvcExt.UploadFile alteredFile = file;
        log.debug("Disabled plugins: {}", disabledPlugins);
        for (OssServiceProperties.PluginConfig plugin : plugins) {
            if (!disabledPlugins.contains(plugin.getId()) && plugin.getFileTypes().contains(ext)
                    && plugin.instance().supported(ext)) {
                alteredFile = plugin.instance().alter(alteredFile, path, ossBackend, plugin.props());
            }
        }
        return alteredFile;
    }

}
