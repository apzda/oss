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

import com.apzda.cloud.oss.config.OssClientHelper;
import com.apzda.cloud.oss.proto.*;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Service
@RequiredArgsConstructor
public class OssServiceImpl implements OssService {

    @Override
    public QueryRes query(QueryReq request) {
        val ossBackend = OssClientHelper.getOssBackend();
        return null;
    }

    @Override
    public DeleteRes delete(DeleteReq request) {
        return null;
    }

    @Override
    public UploadRes upload(UploadReq request) {
        return null;
    }

    @Override
    public ChunkUploadRes chunkUpload(ChunkUploadReq request) {
        return null;
    }

}
