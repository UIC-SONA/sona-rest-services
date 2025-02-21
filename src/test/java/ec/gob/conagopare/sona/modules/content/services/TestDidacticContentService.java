package ec.gob.conagopare.sona.modules.content.services;

import ec.gob.conagopare.sona.modules.content.dto.DidacticContentDto;
import ec.gob.conagopare.sona.modules.content.models.DidacticContent;
import ec.gob.conagopare.sona.modules.content.repositories.DidacticContentRepository;
import ec.gob.conagopare.sona.test.StorageTestUtils;
import io.github.luidmidev.springframework.web.problemdetails.ProblemDetailsException;
import io.github.luidmidev.storage.Storage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestDidacticContentService {

    @InjectMocks
    private DidacticContentService service;

    @Mock
    private DidacticContentRepository repository;


    @Mock
    private Storage storage;


    @Test
    void create_CuandoDatosSonCorrectos_DebeRetornarElContenidoDidactico() throws IOException {
        // Arrange
        var title = "title";
        var content = "content";
        var image = new MockMultipartFile("image", "image.jpg", "image/jpeg", new byte[0]);

        var dto = new DidacticContentDto();
        dto.setTitle(title);
        dto.setContent(content);
        dto.setImage(image);

        // Mocks
        when(storage.store(any(byte[].class), any(), any())).then(StorageTestUtils.STORAGE_SAVE_ANSWER);
        when(repository.save(any())).then(invocation -> invocation.getArgument(0));

        // Act
        var result = service.create(dto);

        // Assert
        assertNotNull(result, "El resultado no debe ser nulo");
        assertEquals(title, result.getTitle(), "El título debe ser el mismo");
        assertEquals(content, result.getContent(), "El contenido debe ser el mismo");
        assertNotNull(result.getImage(), "La imagen no debe ser nula");
    }

    @Test
    void create_CuandoNoSeEnviaImagen_DebeArrrojarExcepcion() {
        // Arrange
        var title = "title";
        var content = "content";

        var dto = new DidacticContentDto();
        dto.setTitle(title);
        dto.setContent(content);

        // Act & Assert
        var exception = assertThrows(ProblemDetailsException.class, () -> service.create(dto));
        var body = exception.getBody();
        assertEquals(400, body.getStatus(), "El status debe ser 400");
    }

    @Test
    void update_CuandoDatosSonCorrectos_DebeRetornarElContenidoDidactico() throws IOException {
        // Arrange
        var title = "title";
        var content = "content";
        var image = new MockMultipartFile("image", "image.jpg", "image/jpeg", new byte[0]);

        var dto = new DidacticContentDto();
        dto.setTitle(title);
        dto.setContent(content);
        dto.setImage(image);

        var id = UUID.randomUUID();
        var didacticContent = DidacticContent.builder()
                .id(id)
                .title("old title")
                .content("old content")
                .image("old image")
                .build();

        // Mocks
        when(storage.store(any(byte[].class), any(), any())).then(StorageTestUtils.STORAGE_SAVE_ANSWER);
        when(repository.save(any())).then(invocation -> invocation.getArgument(0));
        when(repository.findById(id)).thenReturn(Optional.of(didacticContent));

        // Act
        var result = service.update(id, dto);

        // Assert
        assertNotNull(result, "El resultado no debe ser nulo");
        assertEquals(title, result.getTitle(), "El título debe ser el mismo");
        assertEquals(content, result.getContent(), "El contenido debe ser el mismo");
        assertNotNull(result.getImage(), "La imagen no debe ser nula");
    }
}
