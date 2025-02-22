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
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
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

    @Test
    void createUpdateGetImageAndRemoveTip() throws Exception {

        var accessToken = obtainAdminBearerToken();

        var title = "title";
        var summary = "summary";
        var tags = "tag1, tag2";
        var active = false;
        var description = "description";
        var imageFile = new ClassPathResource("image-for-tests.jpg").getFile();
        var image = new MockMultipartFile(
                "image",
                imageFile.getName(),
                "image/jpeg",
                FileUtils.readFileToByteArray(imageFile)
        );

        var tipJson = mockMvc.perform(multipart("/content/tips")
                        .file(image)
                        .part(
                                new MockPart("title", title.getBytes()),
                                new MockPart("summary", summary.getBytes()),
                                new MockPart("tags", tags.getBytes()),
                                new MockPart("active", String.valueOf(active).getBytes()),
                                new MockPart("description", description.getBytes())
                        )
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var tipId = JsonPath.read(tipJson, "$.id");

        mockMvc.perform(get("/content/tips/{id}/image", tipId)
                        .header("Authorization", accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(multipart("/content/tips/{id}", tipId)
                        .file(image)
                        .part(
                                new MockPart("title", "new title".getBytes()),
                                new MockPart("summary", "new summary".getBytes()),
                                new MockPart("tags", "new tag1, new tag2".getBytes()),
                                new MockPart("active", String.valueOf(!active).getBytes()),
                                new MockPart("description", "new description".getBytes())
                        )
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .header("Authorization", accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/content/tips/{id}", tipId)
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("new description"));

        mockMvc.perform(delete("/content/tips/{id}/image", tipId)
                        .header("Authorization", accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/content/tips/{id}", tipId)
                        .header("Authorization", accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/content/tips/{id}", tipId)
                        .header("Authorization", accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getActivesTips() throws Exception {
        var accessToken = obtainAdminBearerToken();

        var title = "title";
        var summary = "summary";
        var tags = "tag1, tag2";
        var active = true;
        var description = "description";
        var imageFile = new ClassPathResource("image-for-tests.jpg").getFile();

        var image = new MockMultipartFile(
                "image",
                imageFile.getName(),
                "image/jpeg",
                FileUtils.readFileToByteArray(imageFile)
        );

        mockMvc.perform(multipart("/content/tips")
                        .file(image)
                        .part(
                                new MockPart("title", title.getBytes()),
                                new MockPart("summary", summary.getBytes()),
                                new MockPart("tags", tags.getBytes()),
                                new MockPart("active", String.valueOf(active).getBytes()),
                                new MockPart("description", description.getBytes())
                        )
                        .header("Authorization", accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/content/tips/actives")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].active", everyItem(is(true))));
    }

    @Test
    void rateTipsAndGetTopRated() throws Exception {
        var accessToken = obtainAdminBearerToken();

        var title = "title for rate";
        var summary = "summary";
        var tags = "tag1, tag2";
        var active = true;
        var description = "description";
        var imageFile = new ClassPathResource("image-for-tests.jpg").getFile();

        var image = new MockMultipartFile(
                "image",
                imageFile.getName(),
                "image/jpeg",
                FileUtils.readFileToByteArray(imageFile)
        );

        var tipJson = mockMvc.perform(multipart("/content/tips")
                        .file(image)
                        .part(
                                new MockPart("title", title.getBytes()),
                                new MockPart("summary", summary.getBytes()),
                                new MockPart("tags", tags.getBytes()),
                                new MockPart("active", String.valueOf(active).getBytes()),
                                new MockPart("description", description.getBytes())
                        )
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var tipId = JsonPath.read(tipJson, "$.id");

        mockMvc.perform(post("/content/tips/rate/{id}", tipId)
                        .param("value", "5")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/content/tips/top")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andDo(result -> log.info(result.getResponse().getContentAsString()))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(tipId));
    }

}