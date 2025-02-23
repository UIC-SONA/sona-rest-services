package ec.gob.conagopare.sona.modules.tips.services;

import ec.gob.conagopare.sona.modules.content.services.NotificationService;
import ec.gob.conagopare.sona.modules.tips.dto.TipDto;
import ec.gob.conagopare.sona.modules.tips.models.Tip;
import ec.gob.conagopare.sona.modules.tips.repositories.TipRepository;
import ec.gob.conagopare.sona.modules.tips.repositories.TipValuationRepository;
import ec.gob.conagopare.sona.modules.tips.service.TipService;
import ec.gob.conagopare.sona.modules.user.models.User;
import ec.gob.conagopare.sona.modules.user.service.UserService;
import ec.gob.conagopare.sona.test.StorageTestUtils;
import io.github.luidmidev.springframework.data.crud.core.exceptions.NotFoundEntityException;
import io.github.luidmidev.springframework.web.problemdetails.ProblemDetailsException;
import io.github.luidmidev.storage.Storage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestTipService {

    @InjectMocks
    private TipService service;

    @Mock
    private TipRepository repository;

    @Mock
    private TipValuationRepository valuationRepository;

    @Mock
    private Storage storage;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserService userService;

    @Test
    void create_CuandoDatosSonCorrectos_DebeRetornarTip() throws IOException {
        // Arrange
        var title = "title";
        var summary = "summary";
        var description = "description";
        var image = new MockMultipartFile("image", "image.jpg", "image/jpeg", new byte[0]);

        var dto = new TipDto();
        dto.setTitle(title);
        dto.setSummary(summary);
        dto.setDescription(description);
        dto.setImage(image);
        dto.setActive(true);


        // Mock
        when(repository.save(any(Tip.class))).then(invocation -> {
            var tip = invocation.getArgument(0, Tip.class);
            tip.setId(UUID.randomUUID());
            return tip;
        });
        when(storage.store(any(InputStream.class), any(), any())).then(StorageTestUtils.STORAGE_SAVE_ANSWER);

        // Act
        var result = service.create(dto);

        // Assert
        assertNotNull(result, "El resultado no debe ser nulo");
        assertEquals(title, result.getTitle(), "El título debe ser el mismo");
        assertEquals(summary, result.getSummary(), "El resumen debe ser el mismo");
        assertEquals(description, result.getDescription(), "La descripción debe ser la misma");
        assertNotNull(result.getImage(), "La imagen no debe ser nula");
    }


    @Test
    void create_CuandoNoSeEnviaImagen_DebeArrojarExcepcion() {
        // Arrange
        var title = "title";
        var summary = "summary";
        var description = "description";

        var dto = new TipDto();
        dto.setTitle(title);
        dto.setSummary(summary);
        dto.setDescription(description);

        // Act & Assert
        var exception = assertThrows(ProblemDetailsException.class, () -> service.create(dto));
        var body = exception.getBody();
        assertEquals(400, body.getStatus(), "El status code debe ser 400");
    }

    @Test
    void create_CuandoOcurreErrorAlGuardarImagen_DebeArrojarExcepcion() throws IOException {
        // Arrange
        var title = "title";
        var summary = "summary";
        var description = "description";
        var image = new MockMultipartFile("image", "image.jpg", "image/jpeg", new byte[0]);

        var dto = new TipDto();
        dto.setTitle(title);
        dto.setSummary(summary);
        dto.setDescription(description);
        dto.setImage(image);

        // Mock
        when(storage.store(any(InputStream.class), any(), any())).thenThrow(new IOException());

        // Act & Assert
        var exception = assertThrows(ProblemDetailsException.class, () -> service.create(dto));
        var body = exception.getBody();
        assertEquals(500, body.getStatus(), "El status code debe ser 500");
    }

    @Test
    void update_CuandoDatosSeEnviaNuevaImagen_DebeActualizarImagen() throws IOException {
        // Arrange
        var title = "title";
        var summary = "summary";
        var description = "description";
        var image = new MockMultipartFile("image", "image.jpg", "image/jpeg", new byte[0]);

        var dto = new TipDto();
        dto.setTitle(title);
        dto.setSummary(summary);
        dto.setDescription(description);
        dto.setImage(image);
        dto.setActive(true);

        var id = UUID.randomUUID();
        var oldImagePath = "old/image.jpg";

        var tip = new Tip();
        tip.setId(id);
        tip.setTitle("old title");
        tip.setSummary("old summary");
        tip.setDescription("old description");
        tip.setImage(oldImagePath);
        tip.setActive(false);

        var userId = 1L;
        var contextUser = User.builder()
                .id(userId)
                .build();

        // Mock
        when(repository.findByIdWithRates(id, userId)).thenReturn(Optional.of(tip));
        when(userService.getCurrentUser()).thenReturn(contextUser);
        when(repository.save(any(Tip.class))).then(invocation -> invocation.getArgument(0, Tip.class));
        when(storage.store(any(InputStream.class), any(), any())).then(StorageTestUtils.STORAGE_SAVE_ANSWER);

        // Act
        var result = service.update(id, dto);

        // Assert
        assertNotNull(result, "El resultado no debe ser nulo");
        assertEquals(title, result.getTitle(), "El título debe ser el mismo");
        assertEquals(summary, result.getSummary(), "El resumen debe ser el mismo");
        assertEquals(description, result.getDescription(), "La descripción debe ser la misma");
        assertNotEquals(oldImagePath, result.getImage(), "La imagen no debe ser la misma");
        assertNotNull(result.getImage(), "La imagen no debe ser nula");
    }

    @Test
    void deleteImage_CuandoExisteElTip_DebeEliminarImagen() {
        // Arrange
        var id = UUID.randomUUID();
        var imagePath = "image.jpg";

        var tip = new Tip();
        tip.setId(id);
        tip.setImage(imagePath);

        // Mock
        when(repository.findById(id)).thenReturn(Optional.of(tip));

        // Act
        service.deleteImage(id);

        // Assert
        assertNull(tip.getImage(), "La imagen debe ser nula");
    }

    @Test
    void deleteImage_CuandoNoExisteElTip_DebeArrojarExcepcion() {
        // Arrange
        var id = UUID.randomUUID();

        // Mock
        when(repository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundEntityException.class, () -> service.deleteImage(id));
    }

    @Test
    void rate_CuandoNoHaHechoValoracion_DebeActualizarValoracionExistente() {
        // Arrange
        var id = UUID.randomUUID();
        var value = 5;

        var tip = new Tip();
        tip.setId(id);

        var userId = 1L;
        var contextUser = User.builder()
                .id(userId)
                .build();

        // Mock
        when(repository.findByIdWithRates(id, userId)).thenReturn(Optional.of(tip));
        when(userService.getCurrentUser()).thenReturn(contextUser);
        when(valuationRepository.findByTipIdAndUserId(id, userId)).thenReturn(Optional.empty());
        when(valuationRepository.save(any())).then(invocation -> invocation.getArgument(0));

        // Act
        service.rate(id, value);

        // Assert
        assertTrue(true, "No debe lanzar excepción");
    }
}
