package com.apzda.cloud.oss.resumable;

import com.apzda.cloud.oss.proto.ChunkUploadReq;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.util.HashMap;

public class ResumableStorage {

    private ResumableStorage() {
    }

    private static ResumableStorage sInstance;

    public static synchronized ResumableStorage getInstance() {
        if (sInstance == null) {
            sInstance = new ResumableStorage();
        }
        return sInstance;
    }

    private final HashMap<String, ResumableInfo> mMap = new HashMap<>();

    public synchronized ResumableInfo get(int resumableChunkSize, long resumableTotalSize, String resumableIdentifier,
            String resumableFilename, String resumableFilePath) {

        ResumableInfo info = mMap.get(resumableIdentifier);

        if (info == null) {
            info = new ResumableInfo();

            info.resumableChunkSize = resumableChunkSize;
            info.resumableTotalSize = resumableTotalSize;
            info.resumableIdentifier = resumableIdentifier;
            info.resumableFilename = resumableFilename;
            info.resumableFilePath = resumableFilePath;

            mMap.put(resumableIdentifier, info);
        }
        return info;
    }

    public void remove(ResumableInfo info) {
        mMap.remove(info.resumableIdentifier);
    }

    public ResumableInfo getResumableInfo(ChunkUploadReq req, String baseDir) {

        int resumableChunkSize = req.getChunkSize();
        long resumableTotalSize = req.getTotalSize();
        String resumableIdentifier = req.getFileId();
        String resumableFilename = req.getFileName();
        String resumableFilePath = new File(baseDir, resumableFilename).getAbsolutePath() + ".temp";

        ResumableInfo info = get(resumableChunkSize, resumableTotalSize, resumableIdentifier, resumableFilename,
                resumableFilePath);

        if (!info.valid()) {
            remove(info);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Invalid request params: chunkSize=%d, totalSize=%d, fileId=%s,fileName=%s ",
                            resumableChunkSize, resumableTotalSize, resumableIdentifier, resumableFilename));
        }
        return info;
    }

}
