package com.apzda.cloud.oss.plugin.watermark;

import cn.hutool.core.io.FileUtil;
import com.apzda.cloud.gsvc.config.Props;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.apzda.cloud.oss.config.BackendConfig;
import com.apzda.cloud.oss.fs.backend.FsBackend;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
class WatermarkPluginTest {

    @Test
    void alter() throws Exception {
        // given
        val ossBackend = new FsBackend();
        val backendConfig = new BackendConfig();
        backendConfig.setRootDir("./src/test/");
        ossBackend.init(backendConfig);
        val watermarkPlugin = new WatermarkPlugin();

        val builder = GsvcExt.UploadFile.newBuilder();
        builder.setFile("./src/test/cats.jpg");
        builder.setExt("jpg");
        val file = builder.build();

        // when
        val altered = watermarkPlugin.alter(file, null, ossBackend, new Props(new HashMap<>() {
            {
                put(WatermarkPlugin.PROP_WATERMARK_S, "/watermark.png");
                put(WatermarkPlugin.PROP_TMPDIR_S, FileUtil.getTmpDirPath());
                put(WatermarkPlugin.PROP_KEEP_B, "true");
                put(WatermarkPlugin.PROP_NOISE_I, "0");
                put(WatermarkPlugin.PROP_POS_S, "rd");
                put(WatermarkPlugin.PROP_OPACITY_D, "0.55");
                put(WatermarkPlugin.PROP_ROTATE_I, "60");
            }
        }));

        // then
        val filePath = altered.getFile();
        assertThat(filePath).isNotBlank();
        assertThat(filePath).isNotEqualTo(file.getFile());
    }

}
