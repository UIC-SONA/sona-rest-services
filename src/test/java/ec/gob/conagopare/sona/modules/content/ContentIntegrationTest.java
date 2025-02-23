package ec.gob.conagopare.sona.modules.content;

import com.jayway.jsonpath.JsonPath;
import ec.gob.conagopare.sona.test.IntegrationTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContentIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;


    @Test
    void createUpdateGetImageAndRemoveDidacticContent() throws Exception {

        var accessToken = obtainAdminBearerToken();

        var title = "title";
        var content = "content";
        var imageFile = new ClassPathResource("image-for-tests.jpg").getFile();
        var image = new MockMultipartFile(
                "image",
                imageFile.getName(),
                "image/jpeg",
                FileUtils.readFileToByteArray(imageFile)
        );

        var didacticCOntenJson = mockMvc.perform(multipart("/content/didactic")
                        .file(image)
                        .param("title", title)
                        .param("content", content)
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var didacticContentId = JsonPath.read(didacticCOntenJson, "$.id");

        mockMvc.perform(get("/content/didactic/{id}/image", didacticContentId)
                        .header("Authorization", accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(multipart("/content/didactic/{id}", didacticContentId)
                        .file(image)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .param("title", title)
                        .param("content", "new content")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/content/didactic/{id}", didacticContentId)
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("new content"));

        mockMvc.perform(delete("/content/didactic/{id}", didacticContentId)
                        .header("Authorization", accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/content/didactic/{id}", didacticContentId)
                        .header("Authorization", accessToken))
                .andExpect(status().isNotFound());
    }
}