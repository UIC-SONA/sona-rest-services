package ec.gob.conagopare.sona.modules.bot.service;

import com.google.cloud.dialogflow.cx.v3.*;
import ec.gob.conagopare.sona.modules.bot.ChaBotConfig;
import ec.gob.conagopare.sona.modules.bot.repositories.PromptResponseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestDialogFlowGXChatBotService {

    @Mock
    private ChaBotConfig config;

    @Mock
    private PromptResponseRepository promptResponseRepository;

    @Mock
    private SessionsClient sessionsClient;

    @Mock
    private DetectIntentResponse detectIntentResponse;

    @Mock
    private QueryResult queryResult;

    @InjectMocks
    private DialogFlowGXChatBotService chatBotService;

    @BeforeEach
    void setup() {
        LocaleContextHolder.setLocale(java.util.Locale.ENGLISH);
        var session = new ChaBotConfig.Session();
        session.setProject("project");
        session.setLocation("location");
        session.setAgent("agent");
        lenient().when(config.getSession()).thenReturn(session);
    }

    @Test
    void shouldReturnValidResponse() {
        var responseMessage = ResponseMessage.newBuilder()
                .setText(ResponseMessage.Text.newBuilder().addText("Hello! How can I help you?").build())
                .build();

        when(detectIntentResponse.getQueryResult()).thenReturn(queryResult);
        when(queryResult.getResponseMessagesList()).thenReturn(List.of(responseMessage));
        when(sessionsClient.detectIntent(any())).thenReturn(detectIntentResponse);

        try (var staticMock = mockStatic(SessionsClient.class)) {
            staticMock.when(() -> SessionsClient.create(any(SessionsSettings.class))).thenReturn(sessionsClient);
            var response = chatBotService.internalChat("session123", "Hello");

            assertFalse(response.isEmpty());
            assertEquals("Hello! How can I help you?", response.getFirst());
        }
    }

    @Test
    void shouldHandleEmptyResponse() throws IOException {
        when(detectIntentResponse.getQueryResult()).thenReturn(queryResult);
        when(queryResult.getResponseMessagesList()).thenReturn(List.of());
        when(sessionsClient.detectIntent(any())).thenReturn(detectIntentResponse);

        try (var staticMock = mockStatic(SessionsClient.class)) {
            staticMock.when(() -> SessionsClient.create(any(SessionsSettings.class))).thenReturn(sessionsClient);
            var response = chatBotService.internalChat("session123", "Hello");

            assertEquals(List.of("Lo siento, no puedo responder en este momento"), response);
        }
    }

    @Test
    void shouldHandleErrorGracefully() {
        given(sessionsClient.detectIntent(any())).willAnswer(invocation -> {
            throw new IOException("Error");
        });

        try (var staticMock = mockStatic(SessionsClient.class)) {
            staticMock.when(() -> SessionsClient.create(any(SessionsSettings.class))).thenReturn(sessionsClient);
            var response = chatBotService.internalChat("session123", "Hello");

            assertTrue(response.getFirst().contains("ocurrió un error"));
        }

    }
}
