package com.apzda.cloud.oss.minio.backend;

import com.apzda.cloud.oss.config.BackendConfig;
import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
class MinioBackendTest {

    private static MinioBackend ossBackend;

    @BeforeAll
    static void init() {
        val config = new BackendConfig();
        config.setAccessKey("fXqu6wy8ps2yAz8M");
        config.setSecretKey("ltFSRt20HzcHQ3qLfkG4Qhd9zKY6NANs");
        config.setEndpoint("http://127.0.0.1:39000");
        config.setBucketName("bucket-test");
        config.setPathPatten("yyyy");
        ossBackend = new MinioBackend();
        assertThat(ossBackend.init(config)).isTrue();
    }

    @Test
    void getFile() {
    }

    @Test
    void uploadFile() throws IOException {
        // given
        val file = new File("pom.xml");
        // when
        val fileInfo = ossBackend.uploadFile(file);
        // then
        assertThat(fileInfo).isNotNull();
        assertThat(fileInfo.getFilename()).isEqualTo("pom.xml");
        assertThat(fileInfo.getExt()).isEqualTo("xml");
    }

    @Test
    void delete() {
    }

}
