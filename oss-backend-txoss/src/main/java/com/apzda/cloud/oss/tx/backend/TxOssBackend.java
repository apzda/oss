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
import com.apzda.cloud.oss.file.IOssFile;
import com.apzda.cloud.oss.proto.FileInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public class TxOssBackend implements OssBackend {
    @Override
    public IOssFile getFile(String filePath) throws IOException {
        return null;
    }

    @Override
    public FileInfo uploadFile(File file, String path) throws IOException {
        return null;
    }

    @Override
    public FileInfo uploadFile(InputStream stream, String fileName, String path) throws IOException {
        return null;
    }

    @Override
    public boolean delete(String filePath) throws IOException {
        return false;
    }
}
