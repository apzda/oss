package com.apzda.cloud.oss.fs.file;

import com.apzda.cloud.oss.config.BackendConfig;
import com.apzda.cloud.oss.fs.backend.FsBackend;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
class FsFileTest {

    @Test
    void fs_file_should_be_work() throws IOException {
        // given
        val config = new BackendConfig();
        config.setRootDir("./");
        config.setPathPatten("yyyy");
        val backend = new FsBackend();
        backend.init(config);
        val file = new FsFile("/pom.xml", backend);
        // when
        val stat = file.stat();
        // then
        assertThat(stat).isNotNull();
        assertThat(stat.getExt()).isEqualTo("xml");
        assertThat(stat.getExist()).isTrue();
        assertThat(stat.getFileId()).isNotBlank();
        assertThat(stat.getContentType()).isEqualTo("application/xml");
        // when
        val localFile = file.getLocalFile();
        assertThat(localFile).isNotNull();
        assertThat(localFile.exists()).isTrue();

        // when
        val inputStream = file.getInputStream();
        // then
        assertThat(inputStream).isNotNull();
        // when
        val string = FileCopyUtils.copyToString(new InputStreamReader(inputStream));
        // then
        assertThat(string).contains("oss-backend-fs");

        inputStream.close();
    }

}
