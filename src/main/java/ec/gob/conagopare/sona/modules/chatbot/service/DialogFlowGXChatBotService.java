package ec.gob.conagopare.sona.modules.chatbot.service;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.dialogflow.cx.v3.*;
import ec.gob.conagopare.sona.modules.chatbot.ChaBotConfig;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ChatBotService {

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

    public


    public List<ResponseMessage> detectIntent(String sessionId, String text) throws IOException {

        if (activeIntentsDetection.contains(sessionId)) {
            return List.of();
        }

        try (var sessionsClient = getSessionsClient()) {

            var textInput = TextInput.newBuilder().setText(text);
            var queryInput = QueryInput.newBuilder()
                    .setText(textInput)
                    .setLanguageCode(LocaleContextHolder.getLocale().getLanguage())
                    .build();

            var request = DetectIntentRequest.newBuilder()
                    .setSession(getSessionName(sessionId).toString())
                    .setQueryInput(queryInput)
                    .build();

            var response = sessionsClient.detectIntent(request);

            var queryResult = response.getQueryResult();
            return queryResult.getResponseMessagesList();
        }
    }


    public SessionName getSessionName(String session) {
        var sessionConfig = config.getSession();
        return SessionName.ofProjectLocationAgentSessionName(
                sessionConfig.getProject(),
                sessionConfig.getLocation(),
                sessionConfig.getAgent(),
                session
        );
    }

    public static SessionsClient getSessionsClient() throws IOException {
        var settings = SessionsSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();

        return SessionsClient.create(settings);
    }

}
