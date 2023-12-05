package com.apzda.cloud.oss.tx.backend;

import com.apzda.cloud.oss.config.BackendConfig;
import lombok.val;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Disabled
class TxCosBackendTest {

    private static TxCosBackend ossBackend;

    @BeforeAll
    static void init() {
        val config = new BackendConfig();
        config.setAccessKey("");
        config.setSecretKey("");
        config.setEndpoint("oss-1251743910.cos.ap-shanghai.myqcloud.com");
        config.setRegion("ap-shanghai");
        config.setBucketName("oss-1251743910");
        config.setPathPatten("yyyy");
        ossBackend = new TxCosBackend();
        assertThat(ossBackend.init(config)).isTrue();
    }

    @AfterAll
    static void tearDown() {
        ossBackend.close();
    }

    @Test
    void getFile() throws IOException {
        val ossFile = ossBackend.getFile("/tmp/file.md");
        // then
        assertThat(ossFile).isNotNull();
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
    void delete() throws IOException {
        val file = new File("src/test/java/com/apzda/cloud/oss/tx/backend/TxCosBackendTest.java");
        // when
        val fileInfo = ossBackend.uploadFile(file);
        // then
        assertThat(fileInfo).isNotNull();

        // when
        val deleted = ossBackend.delete(fileInfo.getPath());
        assertThat(deleted).isTrue();
    }

}
