package com.apzda.cloud.oss.file;

import com.apzda.cloud.gsvc.client.IServiceCaller;
import com.apzda.cloud.oss.TestConfig;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(classes = TestConfig.class)
@ActiveProfiles("test")
@TestPropertySource(properties = { "apzda.cloud.oss.backend=fs", "apzda.cloud.oss.backends.fs.root-dir=./" })
class OssFileTest {

    @MockBean
    private IServiceCaller serviceCaller;

    @Test
    void oss_file_should_work() throws IOException {
        val file = new OssFile("/pom.xml");
        // when
        val stat = file.stat();
        // then
        assertThat(stat).isNotNull();
        assertThat(stat.getExt()).isEqualTo("xml");
        assertThat(stat.getExist()).isTrue();
        assertThat(stat.getFileId()).isNotBlank();
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
        assertThat(string).contains("oss-client");

        inputStream.close();
    }

}
