package ec.gob.conagopare.sona.modules.bot.service;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.dialogflow.cx.v3.*;
import ec.gob.conagopare.sona.modules.bot.ChaBotConfig;
import ec.gob.conagopare.sona.modules.bot.repositories.PromptResponseRepository;
import io.github.luidmidev.springframework.web.problemdetails.ApiError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Service
public class DialogFlowGXChatBotService extends ChatBotService {

    private final ChaBotConfig config;
    private final Set<String> activeIntentsDetection = ConcurrentHashMap.newKeySet();
    private static final Credentials credentials;

    static {
        try (var resourceCredentials = new FileInputStream("google/service_account_dialogflow.json")) {
            credentials = GoogleCredentials.fromStream(resourceCredentials);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public DialogFlowGXChatBotService(ChaBotConfig config, PromptResponseRepository promptResponseRepository) {
        super(promptResponseRepository);
        this.config = config;
    }

    @Override
    protected List<String> internalChat(String session, String prompt) {
        try {
            if (!activeIntentsDetection.add(session)) {
                throw ApiError.badRequest("Ya se está detectando la intención");
            }

            var responses = getResponseMessages(session, prompt).stream()
                    .filter(ResponseMessage::hasText)
                    .flatMap(message -> message.getText().getTextList().stream())
                    .toList();

            return responses.isEmpty() ? List.of("Lo siento, no puedo responder en este momento") : responses;

        } catch (Exception e) {
            log.error("Error al detectar la intención", e);
            return List.of("Lo siento, no puedo responder en este momento, ocurrió un error %s".formatted(e.getMessage()));
        } finally {
            activeIntentsDetection.remove(session);
        }
    }


    private List<ResponseMessage> getResponseMessages(String session, String prompt) throws IOException {
        try (var sessionsClient = getSessionsClient()) {

            var textInput = TextInput.newBuilder().setText(prompt);
            var queryInput = QueryInput.newBuilder()
                    .setText(textInput)
                    .setLanguageCode(LocaleContextHolder.getLocale().getLanguage())
                    .build();

            var request = DetectIntentRequest.newBuilder()
                    .setSession(getSession(session))
                    .setQueryInput(queryInput)
                    .build();

            var response = sessionsClient.detectIntent(request);

            var queryResult = response.getQueryResult();

            return queryResult.getResponseMessagesList();
        }
    }


    public String getSession(String session) {
        var sessionConfig = config.getSession();
        return SessionName.ofProjectLocationAgentSessionName(sessionConfig.getProject(), sessionConfig.getLocation(), sessionConfig.getAgent(), session).toString();
    }

    public static SessionsClient getSessionsClient() throws IOException {
        var settings = SessionsSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();

        return SessionsClient.create(settings);
    }
}
