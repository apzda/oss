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
package com.apzda.cloud.oss.func;

import com.apzda.cloud.oss.config.OssClientHelper;
import lombok.val;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.util.StreamUtils;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public class PreviewHandlerFunction implements HandlerFunction<ServerResponse> {

    private final String pathPrefix;

    public PreviewHandlerFunction(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    @NonNull
    @Override
    public ServerResponse handle(@NonNull ServerRequest request) throws Exception {
        try {
            val file = request.path().replaceFirst(pathPrefix, "/");
            val ossBackend = OssClientHelper.getOssBackend();
            val ossFile = ossBackend.getFile(file);
            val stat = ossFile.stat();
            return ServerResponse.ok()
                .contentLength(stat.getLength())
                .contentType(MediaType.valueOf(stat.getContentType()))
                .build((servletRequest, servletResponse) -> {
                    StreamUtils.copy(ossFile.getInputStream(), servletResponse.getOutputStream());
                    return null;
                });
        }
        catch (Exception e) {
            return ServerResponse.status(404).build();
        }
    }

}
