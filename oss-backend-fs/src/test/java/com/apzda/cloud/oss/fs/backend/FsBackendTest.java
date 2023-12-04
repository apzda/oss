package com.apzda.cloud.oss.fs.backend;

import cn.hutool.core.io.FileUtil;
import com.apzda.cloud.oss.config.BackendConfig;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;

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
        String path;
        try {
            path = backend.generatePath(file, config.getPathPatten(), null);
        }
        catch (FileAlreadyExistsException e) {
            path = e.getFile();
        }
        // when
        val fileInfo = backend.uploadFile(file);
        // then
        assertThat(fileInfo).isNotNull();
        assertThat(fileInfo.getExt()).isEqualTo("xml");
        assertThat(fileInfo.getPath()).isEqualTo(path);

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

        // given
        val file1 = new File("./pom.xml");
        // when
        val fileInfo1 = backend.uploadFile(file);
        // then
        assertThat(fileInfo1).isNotNull();
        assertThat(fileInfo1.getExt()).isEqualTo("xml");
        assertThat(fileInfo1.getPath()).isEqualTo(path);
        assertThat(fileInfo1.getFileId()).isEqualTo(fileInfo.getFileId());
        assertThat(fileInfo1.getFilename()).isEqualTo("pom.xml");

        // when
        val deleted = backend.delete(path);
        // then
        assertThat(deleted).isTrue();

    }

}
