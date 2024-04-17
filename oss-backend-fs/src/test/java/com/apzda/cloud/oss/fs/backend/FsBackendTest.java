package com.apzda.cloud.oss.fs.backend;

import cn.hutool.core.io.FileUtil;
import com.apzda.cloud.oss.config.BackendConfig;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
class FsBackendTest {

    @Test
    void upload_should_success() throws IOException {
        // given
        val config = new BackendConfig();
        config.setRootDir(FileUtil.getTmpDirPath());
        config.setPathPatten("yyyy");
        val backend = new FsBackend();
        // when
        val init = backend.init(config);
        // then
        assertThat(init).isTrue();

        // given
        val file = new File("./pom.xml");
        val path = backend.generatePath(file.getName(), config.getPathPatten(), null);
        // when
        val fileInfo = backend.uploadFile(file);
        // then
        assertThat(fileInfo).isNotNull();
        assertThat(fileInfo.getExt()).isEqualTo("xml");

        // when
        val ossFile = backend.getFile(path);
        // then
        assertThat(ossFile).isNotNull();

        // when
        val stat = ossFile.stat();
        // then
        assertThat(stat).isNotNull();
        assertThat(stat.getExt()).isEqualTo("xml");
        assertThat(stat.getPath()).isEqualTo(path);
        // when

        System.out.println(path);
        val deleted = backend.delete(path);
        // then
        assertThat(deleted).isTrue();

        // given
        val file1 = new File("./pom.xml");
        // when
        val fileInfo1 = backend.uploadFile(file1);
        // then
        assertThat(fileInfo1).isNotNull();
        assertThat(fileInfo1.getExt()).isEqualTo("xml");
        assertThat(fileInfo1.getFileId()).isEqualTo(fileInfo.getFileId());
        assertThat(fileInfo1.getFilename()).isEqualTo("pom.xml");

    }

}
