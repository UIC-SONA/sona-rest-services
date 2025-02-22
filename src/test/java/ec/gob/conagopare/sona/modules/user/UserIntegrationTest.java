package ec.gob.conagopare.sona.modules.user;

import ec.gob.conagopare.sona.modules.user.dto.SingUpUser;
import ec.gob.conagopare.sona.modules.user.dto.UserDto;
import ec.gob.conagopare.sona.modules.user.models.Authority;
import ec.gob.conagopare.sona.test.IntegrationTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final List<Integer> CREATED_USERS = new ArrayList<>();

    @Test
    @Order(1)
    void signUpUser1() throws Exception {
        //
        var username = "juan.perez";
        var password = "Qwerty1598.QERSAW";

        var singUpUser1 = new SingUpUser();

        singUpUser1.setFirstName("JuanOXXX");
        singUpUser1.setLastName("Perez");
        singUpUser1.setUsername(username);
        singUpUser1.setEmail("juan.perez@example.com");
        singUpUser1.setPassword(password);

        var jsonRequest1 = objectMapper.writeValueAsString(singUpUser1);

        mockMvc.perform(post("/user/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Usuario registrado correctamente"));

        var userBearerToken = obtainAccessToken(username, password);

        var userId = getUserId(userBearerToken, mockMvc);
        CREATED_USERS.add(userId);
    }

    @Test
    @Order(2)
    void signUpUser2() throws Exception {
        var username = "maria.gomez";
        var password = "Qwerty1598.QERSAW";

        var singUpUser2 = new SingUpUser();
        singUpUser2.setFirstName(username);
        singUpUser2.setLastName("Gomez");
        singUpUser2.setUsername("maria.gomez");
        singUpUser2.setEmail("maria.gomez@example.com");
        singUpUser2.setPassword("Qwerty1598.QERSAW");

        var jsonRequest2 = objectMapper.writeValueAsString(singUpUser2);

        mockMvc.perform(post("/user/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Usuario registrado correctamente"));

        var userBearerToken = obtainAccessToken(username, password);
        var userId = getUserId(userBearerToken, mockMvc);
        CREATED_USERS.add(userId);
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Usuario registrado correctamente"));

        var userBearerToken = obtainAccessToken("pedro.lopez", "Qwerty1598.QERSAW");

        var userId = getUserId(userBearerToken, mockMvc);
        CREATED_USERS.add(userId);
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

        var adminBearerToken = obtainAdminBearerToken();
        var userId = CREATED_USERS.get(2);

        mockMvc.perform(put("/user/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest3)
                        .header("Authorization", adminBearerToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(5)
    void getUsersMap() throws Exception {
        mockMvc.perform(get("/user/map")
                        .param("ids", CREATED_USERS.stream().map(String::valueOf).toArray(String[]::new)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(3)) // Verificar que la respuesta tenga 3 elementos
                .andExpect(jsonPath("$[*].username", hasItems(
                        "juan.perez",
                        "maria.gomez",
                        "pedro.lopez")));
    }

    @Test
    @Order(6)
    void anonymizeUser() throws Exception {

        var adminBearerToken = obtainAdminBearerToken();
        mockMvc.perform(post("/user/anonymize")
                        .param("anonymize", "true")
                        .header("Authorization", adminBearerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/user/profile")
                        .header("Authorization", adminBearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anonymous").value(true));
    }

    @Test
    @Order(7)
    void changePassword() throws Exception {

        var username = "usuario.cambio";
        var initialPassword = "Qwerty1598.QERSAW";
        var newPassword = "Qwerty1598.QERSAW2";

        var singUpUser = new SingUpUser();
        singUpUser.setFirstName("Usuario");
        singUpUser.setLastName("A Cambiar Contraseña");
        singUpUser.setUsername(username);
        singUpUser.setEmail("user.cambio@hola.com");
        singUpUser.setPassword(initialPassword);

        var jsonRequest = objectMapper.writeValueAsString(singUpUser);

        mockMvc.perform(post("/user/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk());


        var userBearerToken = "Bearer " + obtainAccessToken(username, initialPassword);

        mockMvc.perform(put("/user/password")
                        .param("newPassword", newPassword)
                        .header("Authorization", userBearerToken))
                .andExpect(status().isOk());

        assertDoesNotThrow(() -> obtainAccessToken(username, newPassword));
    }

    @Test
    @Order(8)
    void postProfilePicture() throws Exception {
        var imageFile = new ClassPathResource("image-for-tests.jpg").getFile();
        var profilePicture = new MockMultipartFile(
                "photo",
                imageFile.getName(),
                "image/jpeg",
                FileUtils.readFileToByteArray(imageFile)
        );
        var adminBearerToken = obtainAdminBearerToken();

        mockMvc.perform(multipart("/user/profile-picture")
                        .file(profilePicture)
                        .header("Authorization", adminBearerToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(9)
    void getProfilePicture() throws Exception {
        var adminBearerToken = obtainAdminBearerToken();

        mockMvc.perform(get("/user/profile-picture")
                        .header("Authorization", adminBearerToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(10)
    void getProfilePictureById() throws Exception {
        var adminBearerToken = obtainAdminBearerToken();

        mockMvc.perform(get("/user/1/profile-picture")
                        .header("Authorization", adminBearerToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(11)
    void deleteProfilePicture() throws Exception {
        var adminBearerToken = obtainAdminBearerToken();

        mockMvc.perform(delete("/user/profile-picture")
                        .header("Authorization", adminBearerToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(12)
    void getProfilePicturNotFound() throws Exception {
        var adminBearerToken = obtainAdminBearerToken();

        mockMvc.perform(get("/user/profile-picture")
                        .header("Authorization", adminBearerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @Order(13)
    void enableUser() throws Exception {
        var adminBearerToken = obtainAdminBearerToken();

        mockMvc.perform(put("/user/enable")
                        .param("id", CREATED_USERS.get(2).toString())
                        .param("value", "false")
                        .header("Authorization", adminBearerToken))
                .andExpect(status().isOk());


        mockMvc.perform(get("/user/{id}", CREATED_USERS.get(2))
                        .header("Authorization", adminBearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

    }

    @Test
    @Order(14)
    void searchUser() throws Exception {
        var adminBearerToken = obtainAdminBearerToken();

        mockMvc.perform(get("/user")
                        .param("search", "JuanOXX")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", adminBearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("juan.perez"));
    }

    @Test
    @Order(15)
    void filterUsersByRole() throws Exception {
        var adminBearerToken = obtainAdminBearerToken();

        mockMvc.perform(get("/user")
                        .param("authorities", "ADMINISTRATIVE")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", adminBearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].username", hasItem("pedro.lopez")));
    }

    @Test
    @Order(16)
    void createUserFromAdmin() throws Exception {
        var adminBearerToken = obtainAdminBearerToken();

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
                .andExpect(status().isOk());
    }
}