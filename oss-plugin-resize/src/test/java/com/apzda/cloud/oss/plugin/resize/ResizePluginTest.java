package com.apzda.cloud.oss.plugin.resize;

import cn.hutool.core.io.FileUtil;
import com.apzda.cloud.gsvc.config.Props;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import lombok.val;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
class ResizePluginTest {

    @Test
    void alter() throws Exception {
        // given
        val resizePlugin = new ResizePlugin();
        val builder = GsvcExt.UploadFile.newBuilder();
        builder.setFile("./src/test/cat.jpeg");
        builder.setExt("jpeg");
        val file = builder.build();
        // when
        val altered = resizePlugin.alter(file, "/", null, new Props(new HashMap<>() {
            {
                put(ResizePlugin.PROP_WIDTH_I, "120");
                put(ResizePlugin.PROP_TMPDIR_S, FileUtil.getTmpDirPath());
                put(ResizePlugin.PROP_KEEP_B, "true");
            }
        }));
        // then
        val image = ImageIO.read(new File(altered.getFile()));
        val width = image.getWidth();
        val height = image.getHeight();
        assertThat(width).isEqualTo(120);
        assertThat(height).isEqualTo((120 * 183 / 276));
    }

    @Test
    void supported() {
        // given
        val resizePlugin = new ResizePlugin();
        val ext = "png";
        val ext1 = "exe";
        // when
        val supported = resizePlugin.supported(ext);
        val supported1 = resizePlugin.supported(ext1);
        // then
        assertThat(supported).isTrue();
        assertThat(supported1).isFalse();
    }

}
