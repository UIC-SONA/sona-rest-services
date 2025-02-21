package ec.gob.conagopare.sona.modules.user.service;

import ec.gob.conagopare.sona.application.configuration.keycloak.KeycloakUserManager;
import ec.gob.conagopare.sona.modules.user.dto.SingUpUser;
import ec.gob.conagopare.sona.modules.user.dto.UserDto;
import ec.gob.conagopare.sona.modules.user.models.Authority;
import ec.gob.conagopare.sona.modules.user.models.User;
import ec.gob.conagopare.sona.modules.user.repositories.UserRepository;
import io.github.luidmidev.springframework.web.problemdetails.ProblemDetailsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @InjectMocks
    private UserService service;

    @Mock
    private UserRepository repository;

    @Mock
    private KeycloakUserManager keycloakUserManager;

    @Mock
    private Environment environment;

    @Test
    void getCurrentUser() {
        //Arrange
        var keyclaokId = "123";
        var userId = 1L;
        var user = User.builder()
                .id(userId)
                .keycloakId(keyclaokId)
                .build();

        var jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(keyclaokId)
                .build();

        var authentication = new JwtAuthenticationToken(jwt);

        //Mock
        when(repository.findByKeycloakId("123")).thenReturn(Optional.of(user));

        try (var staticMock = Mockito.mockStatic(SecurityContextHolder.class)) {
            staticMock.when(SecurityContextHolder::getContext).thenReturn(new SecurityContextImpl(authentication));

            //Act
            var currentUser = service.getCurrentUser();

            //Assert
            assertEquals(userId, currentUser.getId());
            assertEquals(keyclaokId, currentUser.getKeycloakId());
        }
    }

    @Test
    void getCurrentUser_CuandoUsuarioNoExiste_DebeLanzarExcepcion() {

        //Arrange
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("123")
                .build();

        var authentication = new JwtAuthenticationToken(jwt);

        //Mock
        when(repository.findByKeycloakId("123")).thenReturn(Optional.empty());

        try (var staticMock = Mockito.mockStatic(SecurityContextHolder.class)) {
            staticMock.when(SecurityContextHolder::getContext).thenReturn(new SecurityContextImpl(authentication));

            //Act
            var exception = assertThrows(ProblemDetailsException.class, () -> service.getCurrentUser());
            var body = exception.getBody();

            //Assert
            assertEquals(404, body.getStatus());
        }
    }


    @Test
    void signUp_CuandoLosDatosSonValidos_DebeRetornarElUsuarioCreado() {
        //Arrange
        var email = "jose@test.com";
        var username = "jose";

        var signUp = new SingUpUser();
        signUp.setFirstName("John");
        signUp.setLastName("Doe");
        signUp.setEmail(email);
        signUp.setUsername(username);
        signUp.setPassword("Qwerty1598.AEIUO");

        var keycloakId = "123";

        //Mock
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(keycloakUserManager.create(any())).thenReturn(keycloakId);
        when(keycloakUserManager.searchByEmail(email)).thenReturn(Optional.empty());
        when(keycloakUserManager.searchByUsername(username)).thenReturn(Optional.empty());

        //Act & Assert
        assertDoesNotThrow(() -> service.signUp(signUp));
    }

    @Test
    void signUp_CuandoUsuarioYaExiste_DebeLanzarExcepcion() {
        //Arrange
        var email = "jose@test.com";
        var username = "jose";

        var signUp = new SingUpUser();
        signUp.setFirstName("John");
        signUp.setLastName("Doe");
        signUp.setEmail(email);
        signUp.setUsername(username);
        signUp.setPassword("Qwerty1598.AEIUO");

        //Mock
        when(keycloakUserManager.searchByEmail(email)).thenReturn(Optional.empty());
        when(keycloakUserManager.searchByUsername(username)).thenReturn(Optional.of(new UserRepresentation()));

        //Act & Assert
        var exception = assertThrows(ProblemDetailsException.class, () -> service.signUp(signUp));
        var body = exception.getBody();
        assertEquals(409, body.getStatus());
    }

    @Test
    void signUp_CuandoEmailYaExiste_DebeLanzarExcepcion() {
        //Arrange
        var email = "jose@test.com";
        var username = "jose";

        var signUp = new SingUpUser();
        signUp.setFirstName("John");
        signUp.setLastName("Doe");
        signUp.setEmail(email);
        signUp.setUsername(username);
        signUp.setPassword("Qwerty1598.AEIUO");

        //Mock
        when(keycloakUserManager.searchByEmail(email)).thenReturn(Optional.of(new UserRepresentation()));

        //Act & Assert
        var exception = assertThrows(ProblemDetailsException.class, () -> service.signUp(signUp));
        var body = exception.getBody();
        assertEquals(409, body.getStatus());
    }


    @Test
    void createUser_CuandoLosDatosSonValidos_DebeRetornarElUsuarioCreado() {
        //Arrange
        var keycloakId = "123";
        var firstName = "John";
        var lastName = "Doe";
        var email = "jong@test.com";
        var username = "jong";

        var dto = new UserDto();

        dto.setFirstName(firstName);
        dto.setLastName(lastName);
        dto.setEmail(email);
        dto.setUsername(username);
        dto.setPassword("Qadasd1571.asd");
        dto.setAuthoritiesToAdd(Set.of(Authority.MEDICAL_PROFESSIONAL));

        // Mock
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(keycloakUserManager.create(any())).thenReturn(keycloakId);
        when(keycloakUserManager.searchByEmail(email)).thenReturn(Optional.empty());
        when(keycloakUserManager.searchByUsername(username)).thenReturn(Optional.empty());
        when(environment.acceptsProfiles(Profiles.of("mockTest"))).thenReturn(true);

        // Act
        var user = service.create(dto);

        // Assert
        assertNotNull(user);
    }

    @Test
    void createUser_CuandoNoEnvioAuthorities_DebeLanzarExcepcion() {
        // Arrange
        var firstName = "John";
        var lastName = "Doe";
        var email = "jong@test.com";
        var username = "jong";

        var dto = new UserDto();

        dto.setFirstName(firstName);
        dto.setLastName(lastName);
        dto.setEmail(email);
        dto.setUsername(username);
        dto.setPassword("Qadasd1571.asd");
        dto.setAuthoritiesToAdd(Collections.emptySet());

        // Mock
        when(keycloakUserManager.searchByEmail(email)).thenReturn(Optional.empty());
        when(keycloakUserManager.searchByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        var exception = assertThrows(ProblemDetailsException.class, () -> service.create(dto));
        var body = exception.getBody();
        assertEquals(400, body.getStatus());
    }

    @Test
    void createUser_CuandoNoEnvioPassword_DebeLanzarExcepcion() {
        // Arrange
        var firstName = "John";
        var lastName = "Doe";
        var email = "jong@test.com";
        var username = "jong";

        var dto = new UserDto();
        dto.setFirstName(firstName);
        dto.setLastName(lastName);
        dto.setEmail(email);
        dto.setUsername(username);
        dto.setAuthoritiesToAdd(Set.of(Authority.MEDICAL_PROFESSIONAL));
        dto.setPassword(null);

        // Mock
        when(keycloakUserManager.searchByEmail(email)).thenReturn(Optional.empty());
        when(keycloakUserManager.searchByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        var exception = assertThrows(ProblemDetailsException.class, () -> service.create(dto));
        var body = exception.getBody();
        assertEquals(400, body.getStatus());
    }

    @Test
    void createUser_CuandoRolesInvalidos_DebeLanzarExcepcion() {
        // Arrange
        // Arrange
        var firstName = "John";
        var lastName = "Doe";
        var email = "jong@test.com";
        var username = "jong";

        var dto = new UserDto();
        dto.setFirstName(firstName);
        dto.setLastName(lastName);
        dto.setEmail(email);
        dto.setUsername(username);
        dto.setAuthoritiesToAdd(Set.of(Authority.MEDICAL_PROFESSIONAL, Authority.LEGAL_PROFESSIONAL));
        dto.setPassword("Qadasd1571.asd");

        // Mock
        when(keycloakUserManager.searchByEmail(email)).thenReturn(Optional.empty());
        when(keycloakUserManager.searchByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        var exception = assertThrows(ProblemDetailsException.class, () -> service.create(dto));
        var body = exception.getBody();
        assertEquals(400, body.getStatus());
    }


    @Test
    void enable_CuandoUsuarioACambiarEstadoEsAdministrador_DebeLanzarExcepcion() {
        // Arrange
        var keycloakId = "123";
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(keycloakId)
                .build();

        var user = User.builder()
                .id(1L)
                .keycloakId(keycloakId)
                .authorities(Set.of(Authority.ADMIN))
                .build();

        var userToEnable = User.builder()
                .id(2L)
                .keycloakId("456")
                .authorities(Set.of(Authority.ADMIN))
                .build();

        // Mock
        when(repository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(user));
        when(repository.findById(2L)).thenReturn(Optional.of(userToEnable));

        // Act & Assert
        var exception = assertThrows(ProblemDetailsException.class, () -> service.enable(2L, true, jwt));
        var body = exception.getBody();
        assertEquals(400, body.getStatus());
    }

    @Test
    void enable_CuandoUsuarioACambiarEstadoEsAdministrativoDesdeAdministrativo_DebeLanzarExcepcion() {
        // Arrange
        var keycloakId = "123";
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(keycloakId)
                .build();

        var user = User.builder()
                .id(1L)
                .keycloakId(keycloakId)
                .authorities(Set.of(Authority.ADMINISTRATIVE))
                .build();

        var userToEnable = User.builder()
                .id(2L)
                .keycloakId("456")
                .authorities(Set.of(Authority.ADMINISTRATIVE))
                .build();

        // Mock
        when(repository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(user));
        when(repository.findById(2L)).thenReturn(Optional.of(userToEnable));

        // Act & Assert
        var exception = assertThrows(ProblemDetailsException.class, () -> service.enable(2L, true, jwt));
        var body = exception.getBody();
        assertEquals(400, body.getStatus());
    }

    @Test
    void enable_CasoExitoso() {
        // Arrange
        var keycloakId = "123";
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(keycloakId)
                .build();

        var user = User.builder()
                .id(1L)
                .keycloakId(keycloakId)
                .authorities(Set.of(Authority.ADMIN))
                .build();

        var userToEnable = User.builder()
                .id(2L)
                .keycloakId("456")
                .authorities(Set.of(Authority.MEDICAL_PROFESSIONAL))
                .build();

        // Mock
        when(repository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(user));
        when(repository.findById(2L)).thenReturn(Optional.of(userToEnable));

        // Act & Assert
        assertDoesNotThrow(() -> service.enable(2L, true, jwt));
    }

    @Test
    void anonymize_CasoExitoso() {
        // Arrange
        var keycloakId = "123";
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(keycloakId)
                .build();

        var user = User.builder()
                .id(1L)
                .keycloakId(keycloakId)
                .anonymous(false)
                .build();

        // Mock
        when(repository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(user));

        // Act & Assert
        assertDoesNotThrow(() -> service.anonymize(jwt, true));
    }
}