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
package com.apzda.cloud.oss.plugin.resize;

import cn.hutool.core.io.FileUtil;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.apzda.cloud.oss.backend.OssBackend;
import com.apzda.cloud.oss.plugin.Plugin;
import com.apzda.cloud.oss.plugin.PluginProps;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class ResizePlugin implements Plugin {

    public static final List<String> SUPPORTED_FILE_TYPES = List.of("png", "jpg", "jpeg", "webp");

    public static final String PROP_WIDTH_I = "width";

    public static final String PROP_HEIGHT_I = "height";

    @Override
    public GsvcExt.UploadFile alter(GsvcExt.UploadFile file, String path, OssBackend ossBackend, PluginProps props)
            throws Exception {
        val ext = file.getExt();
        if (!supported(ext)) {
            log.warn("Cannot support: {}", ext);
            return file;
        }
        val width = props.get(PROP_WIDTH_I);
        val height = props.get(PROP_HEIGHT_I);
        if (width == null && height == null) {
            throw new IllegalArgumentException("width or height cannot be both null");
        }
        val originFile = file.getFile();
        val image = ImageIO.read(new File(originFile));
        val imgWidth = image.getWidth();
        val imgHeight = image.getHeight();

        int dsWidth;
        int dsHeight;

        if (width != null) {
            dsWidth = Integer.parseInt(width);
            if (height == null) {
                dsHeight = BigDecimal.valueOf(dsWidth)
                    .multiply(BigDecimal.valueOf(imgHeight))
                    .divide(BigDecimal.valueOf(imgWidth), MathContext.DECIMAL32)
                    .intValue();
            }
            else {
                dsHeight = Integer.parseInt(height);
            }
        }
        else {
            dsHeight = Integer.parseInt(height);
            dsWidth = BigDecimal.valueOf(dsHeight)
                .multiply(BigDecimal.valueOf(imgWidth))
                .divide(BigDecimal.valueOf(imgHeight), MathContext.DECIMAL32)
                .intValue();
        }

        if (dsWidth >= imgWidth || dsHeight >= imgHeight) {
            log.debug("Cannot resize from ({},{}) to ({},{})", imgWidth, imgHeight, dsWidth, dsHeight);
            return file;
        }

        val scaledImg = image.getScaledInstance(dsWidth, dsHeight, Image.SCALE_SMOOTH);
        val outputImage = new BufferedImage(dsWidth, dsHeight, image.getType());
        val graphics = outputImage.getGraphics();
        if (!graphics.drawImage(scaledImg, 0, 0, null)) {
            graphics.dispose();
            throw new IllegalArgumentException(
                    "Cannot resize image: " + file.getFilename() + ", the image pixels are changing");
        }
        graphics.dispose();
        val tmpImg = FileUtil
            .createTempFile("resize_", "." + file.getExt(), new File(props.get(PROP_TMPDIR_S)).toPath())
            .toAbsolutePath()
            .toString();

        if (!ImageIO.write(outputImage, file.getExt(), new File(tmpImg))) {
            throw new IllegalArgumentException("Cannot resize image: " + file.getFilename() + ", the image format '"
                    + file.getExt() + "' is not supported!");
        }

        val builder = GsvcExt.UploadFile.newBuilder(file);
        builder.setFile(tmpImg);
        if (!props.getBoolean(PROP_KEEP_B, false)) {
            if (FileUtil.del(originFile)) {
                log.trace("Origin file deleted: {}", originFile);
            }
            else {
                log.warn("Cannot delete Origin file: {}", originFile);
            }
        }
        else {
            log.trace("Keep Origin file: {}", originFile);
        }
        return builder.build();
    }

    @Override
    public boolean supported(String extName) {
        return SUPPORTED_FILE_TYPES.contains(extName);
    }

}
