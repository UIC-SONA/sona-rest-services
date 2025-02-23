package ec.gob.conagopare.sona.modules.chat;

import com.jayway.jsonpath.JsonPath;
import ec.gob.conagopare.sona.modules.user.dto.SingUpUser;
import ec.gob.conagopare.sona.test.IntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatTest extends IntegrationTest {

    private static boolean isSetUp = false;

    private static Integer user1Id;
    private static final String USER1_USERNAME = "user1";
    private static final String USER2_USERNAME = "user2";

    private static Integer user2Id;
    private static final String USER1_PASSWORD = "Qwerty1598.1598.";
    private static final String USER2_PASSWORD = "Qwerty1598.1598.";

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        if (isSetUp) return;

        var user1 = new SingUpUser();
        user1.setFirstName("User");
        user1.setLastName("One");
        user1.setUsername(USER1_USERNAME);
        user1.setPassword(USER1_PASSWORD);
        user1.setEmail("one.user@chat.com");
        singUp(user1, mockMvc);

        var accessToken = obtainAccessToken(USER1_USERNAME, USER1_PASSWORD);
        user1Id = getUserId(accessToken, mockMvc);

        var user2 = new SingUpUser();
        user2.setFirstName("User");
        user2.setLastName("Two");
        user2.setUsername(USER2_USERNAME);
        user2.setPassword(USER2_PASSWORD);
        user2.setEmail("two.user@chat.com");
        singUp(user2, mockMvc);

        accessToken = obtainAccessToken(USER2_USERNAME, USER2_PASSWORD);
        user2Id = getUserId(accessToken, mockMvc);

        isSetUp = true;
    }

    @Test
    @Order(1)
    void createChatRoomAndSendTextImageVoice() throws Exception {

        var imageFile = new ClassPathResource("image-for-tests.jpg").getFile();
        var voiceFile = new ClassPathResource("audio-for-tests.mp3").getFile();

        var image = new MockMultipartFile("image", imageFile.getName(), "image/jpeg", FileUtils.readFileToByteArray(imageFile));
        var voice = new MockMultipartFile("voice", voiceFile.getName(), "audio/mpeg", FileUtils.readFileToByteArray(voiceFile));

        var accessToken1 = obtainAccessToken(USER1_USERNAME, USER1_PASSWORD);

        var chatRoomJson = mockMvc.perform(get("/chat/user/{userId}/room", user2Id)
                        .header("Authorization", "Bearer " + accessToken1))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var roomId = JsonPath.read(chatRoomJson, "$.id");

        mockMvc.perform(post("/chat/send/{roomId}", roomId)
                        .header("Authorization", "Bearer " + accessToken1)
                        .param("requestId", "1")
                        .content("Hello World"))
                .andExpect(status().isOk());

        mockMvc.perform(multipart("/chat/send/{roomId}/image", roomId)
                        .file(image)
                        .header("Authorization", "Bearer " + accessToken1)
                        .param("requestId", "2"))
                .andExpect(status().isOk());

        var payloadJson = mockMvc.perform(multipart("/chat/send/{roomId}/voice", roomId)
                        .file(voice)
                        .header("Authorization", "Bearer " + accessToken1)
                        .param("requestId", "3"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var messageId = JsonPath.read(payloadJson, "$.message.message").toString();

        mockMvc.perform(get("/chat/resource")
                        .param("id", messageId)
                        .header("Authorization", "Bearer " + accessToken1))
                .andExpect(status().isOk());

        var accessToken2 = obtainAccessToken(USER2_USERNAME, USER2_PASSWORD);

        mockMvc.perform(get("/chat/rooms")
                        .header("Authorization", "Bearer " + accessToken2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(roomId));

        mockMvc.perform(get("/chat/room/{roomId}", roomId)
                        .header("Authorization", "Bearer " + accessToken2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(roomId));

        mockMvc.perform(get("/chat/room/{roomId}/chunk-count", roomId)
                        .header("Authorization", "Bearer " + accessToken2))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));

        mockMvc.perform(get("/chat/room/{roomId}/last-message", roomId)
                        .header("Authorization", "Bearer " + accessToken2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("VOICE"))
                .andExpect(jsonPath("$.sentBy").value(user1Id));


        var messagesJson = mockMvc.perform(get("/chat/room/{roomId}/messages", roomId)
                        .param("chunk", "1")
                        .header("Authorization", "Bearer " + accessToken2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].message").value("Hello World"))
                .andExpect(jsonPath("$[1].type").value("IMAGE"))
                .andExpect(jsonPath("$[2].type").value("VOICE"))
                .andExpect(jsonPath("$[*].sentBy", everyItem(is(user1Id))))
                .andReturn()
                .getResponse().
                getContentAsString();

        var messagesid = JsonPath.read(messagesJson, "$[*].id");

        mockMvc.perform(put("/chat/room/{roomId}/read", roomId)
                        .header("Authorization", "Bearer " + accessToken2)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(messagesid)))
                .andExpect(status().isOk());


    }

}