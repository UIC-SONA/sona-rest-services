package ec.gob.conagopare.sona.modules.menstrualcycle;

import ec.gob.conagopare.sona.modules.menstrualcycle.dto.CycleDetails;
import ec.gob.conagopare.sona.modules.user.dto.SingUpUser;
import ec.gob.conagopare.sona.test.IntegrationTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MenstrualCycleIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String USERNAME = "maria.basurto";
    private static final String PASSWORD = "Qwerty15981598.";
    private static boolean isSetUp = false;

    @BeforeEach
    void setUp() throws Exception {
        if (isSetUp) return;

        var singUpUser = new SingUpUser();
        singUpUser.setFirstName("Maria");
        singUpUser.setLastName("Basurto");
        singUpUser.setUsername(USERNAME);
        singUpUser.setPassword(PASSWORD);
        singUpUser.setEmail("maria.basurto@mail.com");

        var jsonRequest1 = objectMapper.writeValueAsString(singUpUser);

        mockMvc.perform(post("/user/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest1));

        isSetUp = true;
    }

    @Test
    @Order(1)
    void saveCycleDetails() throws Exception {
        var accessToken = obtainAccessToken(USERNAME, PASSWORD);
        var cycleDetails = new CycleDetails();

        cycleDetails.setCycleLength(28);
        cycleDetails.setPeriodDuration(5);

        var jsonRequest = objectMapper.writeValueAsString(cycleDetails);

        mockMvc.perform(post("/menstrual-cycle/details")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk());
    }


    @Test
    @Order(2)
    void getCycleData() throws Exception {
        var accessToken = obtainAccessToken(USERNAME, PASSWORD);

        mockMvc.perform(get("/menstrual-cycle")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cycleLength").value(28))
                .andExpect(jsonPath("$.periodDuration").value(5))
                .andExpect(jsonPath("$.periodDates.size()").value(0));
    }

    @Test
    @Order(3)
    void savePeriodLogs() throws Exception {
        var accessToken = obtainAccessToken(USERNAME, PASSWORD);
        var periodDates = new String[]{"2025-02-01", "2025-03-01"};

        var jsonRequest = objectMapper.writeValueAsString(periodDates);

        mockMvc.perform(post("/menstrual-cycle/period-logs")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk());
    }

    @Test
    @Order(4)
    void getCycleDataWithPeriodDates() throws Exception {
        var accessToken = obtainAccessToken(USERNAME, PASSWORD);

        mockMvc.perform(get("/menstrual-cycle")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cycleLength").value(28))
                .andExpect(jsonPath("$.periodDuration").value(5))
                .andExpect(jsonPath("$.periodDates.size()").value(2))
                .andExpect(jsonPath("$.periodDates[0]").value("2025-02-01"))
                .andExpect(jsonPath("$.periodDates[1]").value("2025-03-01"));
    }


}