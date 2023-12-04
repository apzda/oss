package com.apzda.cloud.oss.ali.file;

import com.apzda.cloud.oss.ali.backend.AliOssBackend;
import com.apzda.cloud.oss.config.BackendConfig;
import lombok.val;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.util.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Disabled
class AliOssFileTest {

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
        assertThat(ossBackend.close()).isTrue();
    }

    @Test
    void getLocalFile() throws IOException {
        // given
        val file = new File("src/test/java/com/apzda/cloud/oss/ali/file/AliOssFileTest.java");
        // when
        val fileInfo = ossBackend.uploadFile(file);
        // then
        assertThat(fileInfo).isNotNull();

        // given
        val path = fileInfo.getPath();
        val ossFile = new AliOssFile(path, ossBackend);

        // when
        val stat = ossFile.stat();
        // then
        assertThat(stat).isNotNull();
        assertThat(stat.getFileId()).isEqualTo(fileInfo.getFileId());

        // when
        val localFile = ossFile.getLocalFile();
        // then
        assertThat(localFile).isNotNull();

        // given
        val fileId = DigestUtils.md5DigestAsHex(new FileInputStream(localFile));
        // then
        assertThat(fileId).isEqualTo(stat.getFileId());

        // when
        val deleted = ossFile.delete();
        // then
        assertThat(deleted).isTrue();
    }

}
