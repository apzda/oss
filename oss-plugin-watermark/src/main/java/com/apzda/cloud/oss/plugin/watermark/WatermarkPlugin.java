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
package com.apzda.cloud.oss.plugin.watermark;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.apzda.cloud.oss.backend.OssBackend;
import com.apzda.cloud.oss.plugin.Plugin;
import com.apzda.cloud.oss.plugin.PluginProps;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

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
public class WatermarkPlugin implements Plugin {

    public static final List<String> SUPPORTED_POS = List.of("rd", "br", "tr", "bl", "tl", "ct");

    public static final List<String> SUPPORTED_FILE_TYPES = List.of("png", "jpg", "jpeg", "webp");

    public static final String PROP_WATERMARK_S = "watermark";

    public static final String PROP_POS_S = "pos";// r,br,tr,bl,tl,c

    public static final String PROP_MIN_WIDTH_I = "minWidth";

    public static final String PROP_NOISE_I = "noise";

    public static final String PROP_ROTATE_I = "rotate";

    public static final String PROP_OPACITY_D = "opacity";

    @Override
    public GsvcExt.UploadFile alter(GsvcExt.UploadFile file, String path, OssBackend ossBackend, PluginProps props)
            throws Exception {
        val ext = file.getExt();
        if (!supported(ext)) {
            log.warn("Cannot support: {}", ext);
            return file;
        }
        val watermark = props.get(PROP_WATERMARK_S);
        if (watermark == null || StringUtils.isEmpty(watermark)) {
            throw new IllegalArgumentException("watermark image not configured");
        }
        var pos = props.getString(PROP_POS_S, "br").toLowerCase();
        if (!SUPPORTED_POS.contains(pos)) {
            log.warn("Do not support '{}', use 'br' as default", pos);
            pos = "br";
        }
        val width = props.getInt(PROP_MIN_WIDTH_I, 0);
        val noise = props.getInt(PROP_NOISE_I, 30);
        val rotate = props.getInt(PROP_ROTATE_I, 30);
        val opacity = props.getFloat(PROP_OPACITY_D, 1f);

        val originFile = file.getFile();

        val image = ImageIO.read(new File(originFile));
        val imgWidth = image.getWidth();
        val imgHeight = image.getHeight();

        if (imgWidth < width) {
            return file;
        }

        val waterImg = ImageIO.read(ossBackend.getFile(watermark).getLocalFile());
        val waterImage = generateWaterImage(waterImg, opacity, rotate);
        val wImgWidth = waterImage.getWidth();
        val wImgHeight = waterImage.getHeight();

        val point = calculatePos(pos, wImgWidth, wImgHeight, imgWidth, imgHeight, noise);

        val graphics = image.getGraphics();
        if (!graphics.drawImage(waterImage, point.x, point.y, null)) {
            graphics.dispose();
            throw new IllegalArgumentException(
                    "Cannot add watermark to the image: " + file.getFilename() + ", the image pixels are changing");
        }
        graphics.dispose();
        val tmpImg = FileUtil.createTempFile("water_", "." + file.getExt(), new File(props.get(PROP_TMPDIR_S)).toPath())
            .toAbsolutePath()
            .toString();

        if (!ImageIO.write(image, file.getExt(), new File(tmpImg))) {
            throw new IllegalArgumentException("Cannot add watermark to the image: " + file.getFilename()
                    + ", the image format '" + file.getExt() + "' is not supported!");
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

    /**
     * 根据水印位置和噪音计算水印图片坐标.
     */
    Point calculatePos(String pos, int ww, int wh, int iw, int ih, int noise) {
        val point = new Point();
        val random = RandomUtil.getRandom();
        if (pos.equalsIgnoreCase("rd")) {
            val idx = random.nextInt(1, 6);
            pos = SUPPORTED_POS.get(idx);
        }
        point.x = 0;
        point.y = 0;
        int nw = 0;
        int nh = 0;
        if (noise != 0) {
            nw = random.nextInt(Math.abs(noise));
            nh = random.nextInt(Math.abs(noise));
        }
        switch (pos) {
            case "br": // 右下方
                point.x = iw - ww - nw;
                point.y = ih - wh - nh;
                break;
            case "bl": // 左下方
                point.x = nw;
                point.y = ih - wh - nh;
                break;
            case "tl": // 左上方
                point.x = nw;
                point.y = nh;
                break;
            case "tr": // 右上方
                point.x = iw - ww - nw;
                point.y = nh;
                break;
            case "ct": // 中间
                if (random.nextInt() % 2 == 0) {
                    point.x = (iw - ww) / 2 + nw;
                }
                else {
                    point.x = (iw - ww) / 2 - nw;
                }
                if (random.nextInt() % 2 == 0) {
                    point.y = (ih - wh) / 2 - nh;
                }
                else {
                    point.y = (ih - wh) / 2 + nh;
                }
        }
        return point;
    }

    BufferedImage generateWaterImage(BufferedImage waterImg, float opacity, int rotate) {
        val width = waterImg.getWidth();
        val height = waterImg.getHeight();
        val waterG = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        val graphics = waterG.createGraphics();
        if (opacity != 1f) {
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        }
        graphics.drawImage(waterImg, 0, 0, null);
        graphics.dispose();
        if (rotate != 0) {
            val random = RandomUtil.getRandom();
            float randRotate = random.nextFloat(Math.abs(rotate));
            if (random.nextInt() % 2 == 0) {
                randRotate = -randRotate;
            }
            val angle = BigDecimal.valueOf(randRotate)
                .multiply(BigDecimal.valueOf(Math.PI))
                .divide(BigDecimal.valueOf(180), MathContext.DECIMAL32)
                .doubleValue();
            return rotate(waterG, angle);
        }
        return waterG;
    }

    BufferedImage rotate(BufferedImage image, double angle) {
        double sin = Math.abs(Math.sin(angle)), cos = Math.abs(Math.cos(angle));
        int w = image.getWidth(), h = image.getHeight();
        int nW = (int) Math.floor(w * cos + h * sin), nH = (int) Math.floor(h * cos + w * sin);
        val rotated = new BufferedImage(nW, nH, Transparency.TRANSLUCENT);
        val g = rotated.createGraphics();
        g.translate((nW - w) / 2, (nH - h) / 2);
        g.rotate(angle, w / 2d, h / 2d);
        g.drawRenderedImage(image, null);
        g.dispose();
        return rotated;
    }

}
