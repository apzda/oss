package com.apzda.cloud.oss.resumable;

import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;

public class ResumableInfo {

    public int resumableChunkSize;

    public long resumableTotalSize;

    public String resumableIdentifier;

    public String resumableFilename;

    public static class ResumableChunkNumber {

        public ResumableChunkNumber(int number) {
            this.number = number;
        }

        public int number;

        @Override
        public boolean equals(Object obj) {
            return obj instanceof ResumableChunkNumber && ((ResumableChunkNumber) obj).number == this.number;
        }

        @Override
        public int hashCode() {
            return number;
        }

    }

    public HashSet<ResumableChunkNumber> uploadedChunks = new HashSet<>();

    public String resumableFilePath;

    public boolean valid() {
        return resumableChunkSize >= 0 && resumableTotalSize >= 0 && StringUtils.isNotEmpty(resumableIdentifier)
                && StringUtils.isNotEmpty(resumableFilename);
    }

    public boolean checkIfUploadFinished() {
        // check if upload finished
        int count = (int) Math.ceil(((double) resumableTotalSize) / ((double) resumableChunkSize));
        for (int i = 1; i < count; i++) {
            if (!uploadedChunks.contains(new ResumableChunkNumber(i))) {
                return false;
            }
        }

        return true;
    }

}
