package ec.gob.conagopare.sona.modules;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.gob.conagopare.sona.modules.menstrualcycle.dto.CycleDetails;
import ec.gob.conagopare.sona.modules.user.dto.SingUpUser;
import ec.gob.conagopare.sona.modules.user.dto.UserDto;
import ec.gob.conagopare.sona.modules.user.models.Authority;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IntegrationTest extends ec.gob.conagopare.sona.test.IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String adminBearerToken = null;

    @BeforeEach
    void setUp() {
        if (adminBearerToken != null) {
            log.info("Bearer token already obtained");
            return;
        }

        var adminCredentials = getAdminCredentials();
        log.info("Obtaining bearer token for user {}", adminCredentials.username());
        adminBearerToken = "Bearer " + obtainAccessToken(adminCredentials.username(), adminCredentials.password());
        log.info("Bearer token obtained successfully");
    }


    @Test
    @Order(1)
    void signUpUser1() throws Exception {
        var singUpUser1 = new SingUpUser();
        singUpUser1.setFirstName("Juan");
        singUpUser1.setLastName("Perez");
        singUpUser1.setUsername("juan.perez");
        singUpUser1.setEmail("juan.perez@example.com");
        singUpUser1.setPassword("Qwerty1598.QERSAW");

        var jsonRequest1 = objectMapper.writeValueAsString(singUpUser1);

        mockMvc.perform(post("/user/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest1))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.message").value("Usuario registrado correctamente"));
    }

    @Test
    @Order(2)
    void signUpUser2() throws Exception {
        var singUpUser2 = new SingUpUser();
        singUpUser2.setFirstName("Maria");
        singUpUser2.setLastName("Gomez");
        singUpUser2.setUsername("maria.gomez");
        singUpUser2.setEmail("maria.gomez@example.com");
        singUpUser2.setPassword("Qwerty1598.QERSAW");

        var jsonRequest2 = objectMapper.writeValueAsString(singUpUser2);

        mockMvc.perform(post("/user/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest2))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.message").value("Usuario registrado correctamente"));

    }

    @Test
    @Order(3)
    void signUpUser3() throws Exception {
        var singUpUser3 = new SingUpUser();
        singUpUser3.setFirstName("Pedro");
        singUpUser3.setLastName("Lopez");
        singUpUser3.setUsername("pedro.lopez");
        singUpUser3.setEmail("pedro.lopez@example.com");
        singUpUser3.setPassword("Qwerty1598.QERSAW");

        var jsonRequest3 = objectMapper.writeValueAsString(singUpUser3);

        mockMvc.perform(post("/user/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest3))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.message").value("Usuario registrado correctamente"));

    }


    @Test
    @Order(4)
    void updateUser3() throws Exception {
        var user3 = new UserDto();
        user3.setFirstName("Pedrito");
        user3.setLastName("Lopez");
        user3.setUsername("pedro.lopez");
        user3.setEmail("pedro.lopez@example.com");
        user3.setAuthoritiesToAdd(Set.of(Authority.ADMINISTRATIVE));
        user3.setAuthoritiesToRemove(Set.of(Authority.USER));

        var jsonRequest3 = objectMapper.writeValueAsString(user3);

        mockMvc.perform(put("/user/4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest3)
                        .header("Authorization", adminBearerToken))
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    @Order(5)
    void getUsersMap() throws Exception {
        mockMvc.perform(get("/user/map")
                        .param("ids", "1", "2", "3", "4"))
                .andExpect(status().is(HttpStatus.OK.value())) // Esperar respuesta 200 OK
                .andExpect(jsonPath("$.size()").value(4)) // Verificar que la respuesta tenga 3 elementos
                .andExpect(jsonPath("$['1'].username").value("admin")) // Usuario por defecto
                .andExpect(jsonPath("$['2'].username").value("juan.perez")) // Verificar el primer usuario
                .andExpect(jsonPath("$['3'].username").value("maria.gomez")) // Verificar el segundo usuario
                .andExpect(jsonPath("$['4'].username").value("pedro.lopez")) // Verificar el tercer usuario
                .andExpect(jsonPath("$['4'].firstName").value("Pedrito")); // Verificar el cambio de nombre
    }

    @Test
    @Order(6)
    void anonymizeUser() throws Exception {
        mockMvc.perform(post("/user/anonymize")
                        .param("anonymize", "true")
                        .header("Authorization", adminBearerToken))
                .andExpect(status().is(HttpStatus.OK.value()));

        mockMvc.perform(get("/user/profile")
                        .header("Authorization", adminBearerToken))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.anonymous").value(true));
    }

    @Test
    @Order(7)
    void changePassword() throws Exception {
        mockMvc.perform(put("/user/password")
                        .param("newPassword", "Qwerty1598.QERSAW")
                        .header("Authorization", adminBearerToken))
                .andExpect(status().is(HttpStatus.OK.value()));

    }

    @Test
    @Order(8)
    void postProfilePicture() throws Exception {
        var imageFile = new ClassPathResource("admin-photo.jpg").getFile();
        var profilePicture = new MockMultipartFile(
                "photo",
                imageFile.getName(),
                "image/jpeg",
                FileUtils.readFileToByteArray(imageFile)
        );

        mockMvc.perform(multipart("/user/profile-picture")
                        .file(profilePicture)
                        .header("Authorization", adminBearerToken))
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    @Order(9)
    void getProfilePicture() throws Exception {
        mockMvc.perform(get("/user/profile-picture")
                        .header("Authorization", adminBearerToken))
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    @Order(10)
    void getProfilePictureById() throws Exception {
        mockMvc.perform(get("/user/1/profile-picture")
                        .header("Authorization", adminBearerToken))
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    @Order(11)
    void deleteProfilePicture() throws Exception {
        mockMvc.perform(delete("/user/profile-picture")
                        .header("Authorization", adminBearerToken))
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    @Order(12)
    void getProfilePicturNotFound() throws Exception {
        mockMvc.perform(get("/user/profile-picture")
                        .header("Authorization", adminBearerToken))
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @Order(13)
    void enableUser() throws Exception {

        mockMvc.perform(put("/user/enable")
                        .param("id", "4")
                        .param("value", "false")
                        .header("Authorization", adminBearerToken))
                .andExpect(status().is(HttpStatus.OK.value()));


        mockMvc.perform(get("/user/4")
                        .header("Authorization", adminBearerToken))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.enabled").value(false));

    }

    @Test
    @Order(14)
    void searchUser() throws Exception {
        mockMvc.perform(get("/user")
                        .param("search", "juan")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", adminBearerToken))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.content[0].username").value("juan.perez"));
    }

    @Test
    @Order(15)
    void filterUsersByRole() throws Exception {
        mockMvc.perform(get("/user")
                        .param("authorities", "ADMINISTRATIVE")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", adminBearerToken))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.content[0].username").value("pedro.lopez"));
    }

    @Test
    @Order(16)
    void createUserFromAdmin() throws Exception {
        var user = new UserDto();
        user.setFirstName("Michael");
        user.setLastName("Jackson");
        user.setUsername("michael.jackson");
        user.setEmail("mk@aaa.com");
        user.setPassword("Qwerty1598.QERSAW");
        user.setAuthoritiesToAdd(Set.of(Authority.USER));
        user.setAuthoritiesToRemove(Set.of());

        var jsonRequest = objectMapper.writeValueAsString(user);

        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", adminBearerToken))
                .andExpect(status().is(HttpStatus.OK.value()));
    }


    private static final String USERNAME = "maria.basurto";
    private static final String PASSWORD = "Qwerty15981598.";
    private boolean isSetUp = false;

    @BeforeEach
    void setUp(TestInfo testInfo) throws Exception {
        if (!testInfo.getTags().contains("cycle")) return;
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
    @Order(17)
    @Tag("cycle")
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
    @Order(18)
    @Tag("cycle")
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
    @Order(19)
    @Tag("cycle")
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
    @Order(20)
    @Tag("cycle")
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