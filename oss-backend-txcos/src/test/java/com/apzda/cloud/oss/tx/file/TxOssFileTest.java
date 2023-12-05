package com.apzda.cloud.oss.tx.file;

import com.apzda.cloud.oss.config.BackendConfig;
import com.apzda.cloud.oss.tx.backend.TxCosBackend;
import lombok.val;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Disabled
class TxOssFileTest {

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
    void getLocalFile() throws IOException {
        // given
        val file = new File("src/test/java/com/apzda/cloud/oss/tx/file/TxOssFileTest.java");
        // when
        val fileInfo = ossBackend.uploadFile(file);
        // then
        assertThat(fileInfo).isNotNull();

        // given
        val path = fileInfo.getPath();
        val ossFile = new TxOssFile(path, ossBackend);

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
        val content = FileCopyUtils.copyToString(new FileReader(localFile));
        // then
        assertThat(content).contains("oss-1251743910");

        // when
        val deleted = ossFile.delete();
        // then
        assertThat(deleted).isTrue();
    }

}
