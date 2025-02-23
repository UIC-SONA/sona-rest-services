package ec.gob.conagopare.sona.modules.bot;

import com.jayway.jsonpath.JsonPath;
import ec.gob.conagopare.sona.modules.user.dto.SingUpUser;
import ec.gob.conagopare.sona.test.IntegrationTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatBotTest extends IntegrationTest {

    public static boolean isSetUp = false;
    private static final String USER_USERNAME = "Maria.Juana";
    private static final String USER_PASSWORD = "Qwerty1598.1598.";

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        if (isSetUp) return;

        var singUpUser = new SingUpUser();
        singUpUser.setFirstName("Maria");
        singUpUser.setLastName("Juana");
        singUpUser.setUsername(USER_USERNAME);
        singUpUser.setPassword(USER_PASSWORD);
        singUpUser.setEmail("mariajuana@host.com");

        singUp(singUpUser, mockMvc);

        isSetUp = true;
    }

    @Test
    @Order(1)
    void chatWithBot() throws Exception {
        var accessToken = obtainAccessToken(USER_USERNAME, USER_PASSWORD);
        var prompt1 = "Hola, que se debe hacer en caso de violencia de género?";

        var responseJson = mockMvc.perform(post("/chatbot/send-message")
                        .param("prompt", prompt1)
                        .contentType("application/json")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var messages1 = JsonPath.read(responseJson, "$.responses");
        log.info("Messages sent to chatbot [1]: {}", prompt1);
        log.info("Responses from chatbot [1]: {}", messages1);

        var prompt2 = "Hola, cuantos años de carcel puede llegar a tener un violador?";

        responseJson = mockMvc.perform(post("/chatbot/send-message")
                        .param("prompt", prompt2)
                        .contentType("application/json")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var messages2 = JsonPath.read(responseJson, "$.responses");
        log.info("Messages sent to chatbot [2]: {}", prompt2);
        log.info("Responses from chatbot [2]: {}", messages2);
    }

    @Test
    @Order(2)
    void getChatHistory() throws Exception {
        var accessToken = obtainAccessToken(USER_USERNAME, USER_PASSWORD);

        mockMvc.perform(get("/chatbot/history")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].prompt").value("Hola, que se debe hacer en caso de violencia de género?"))
                .andExpect(jsonPath("$[1].prompt").value("Hola, cuantos años de carcel puede llegar a tener un violador?"));
    }

}