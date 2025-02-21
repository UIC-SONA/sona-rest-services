package ec.gob.conagopare.sona.modules.menstrualcycle.services;

import ec.gob.conagopare.sona.modules.menstrualcycle.dto.CycleDetails;
import ec.gob.conagopare.sona.modules.menstrualcycle.models.CycleData;
import ec.gob.conagopare.sona.modules.menstrualcycle.repositories.MenstrualCycleRepository;
import ec.gob.conagopare.sona.modules.user.models.Authority;
import ec.gob.conagopare.sona.modules.user.models.User;
import ec.gob.conagopare.sona.modules.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenstrualCycleServiceUnitTest {

    @InjectMocks
    private MenstrualCycleService service;

    @Mock
    private MenstrualCycleRepository repository;

    @Mock
    private UserService userService;


    @Test
    void getCycle_CuandoElCicloExiste_DeberiaRetornarElCiclo() {
        // Arrange
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("1")
                .build();

        var user = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .authorities(Set.of(Authority.USER))
                .build();

        var cycleData = CycleData.builder()
                .id(UUID.randomUUID())
                .user(user)
                .cycleLength(28)
                .periodDuration(5)
                .build();

        // Mock
        when(userService.getUser(jwt)).thenReturn(user);
        when(repository.findByUser(user)).thenReturn(Optional.of(cycleData));

        // Act
        var result = service.getCycle(jwt);

        // Assert
        assertNotNull(result);
        assertEquals(28, result.getCycleLength());
        assertEquals(5, result.getPeriodDuration());
    }

    @Test
    void saveCycleDetails_CuandoElCicloNoExiste_DeberiaCrearUnNuevoCiclo() {
        // Arrange
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("1")
                .build();

        var user = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .authorities(Set.of(Authority.USER))
                .build();

        var cycleDetails = new CycleDetails();
        cycleDetails.setCycleLength(28);
        cycleDetails.setPeriodDuration(5);

        // Mock
        when(userService.getUser(jwt)).thenReturn(user);
        when(repository.findByUser(user)).thenReturn(Optional.empty());

        // Act & Assert
        assertDoesNotThrow(() -> service.saveCycleDetails(cycleDetails, jwt));
    }

    @Test
    void savePeriodDates_CuandoElCicloNoExiste_DeberiaCrearUnNuevoCiclo() {
        // Arrange
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("1")
                .build();

        var user = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .authorities(Set.of(Authority.USER))
                .build();

        var periodDates = List.of(LocalDate.now(), LocalDate.now().plusDays(5));

        // Mock
        when(userService.getUser(jwt)).thenReturn(user);
        when(repository.findByUser(user)).thenReturn(Optional.empty());

        // Act & Assert
        assertDoesNotThrow(() -> service.savePeriodDates(periodDates, jwt));

    }
}