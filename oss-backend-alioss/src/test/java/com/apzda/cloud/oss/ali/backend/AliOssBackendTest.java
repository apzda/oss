package com.apzda.cloud.oss.ali.backend;

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
class AliOssBackendTest {

    private static AliOssBackend ossBackend;

    @BeforeAll
    static void init() {
        val config = new BackendConfig();
        config.setAccessKey("");
        config.setAccessToken("");
        config.setEndpoint("oss-cn-hangzhou.aliyuncs.com");
        config.setBucketName("jh-mer-files");
        config.setPathPatten("yyyy");
        ossBackend = new AliOssBackend();
        assertThat(ossBackend.init(config)).isTrue();
    }

    @AfterAll
    static void tearDown() {
        ossBackend.close();
    }

    @Test
    void getFile() throws IOException {

        // when
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
        val file = new File("src/test/java/com/apzda/cloud/oss/ali/backend/AliOssBackendTest.java");
        // when
        val fileInfo = ossBackend.uploadFile(file);
        // then
        assertThat(fileInfo).isNotNull();

        // when
        val deleted = ossBackend.delete(fileInfo.getPath());
        assertThat(deleted).isTrue();
    }

}
