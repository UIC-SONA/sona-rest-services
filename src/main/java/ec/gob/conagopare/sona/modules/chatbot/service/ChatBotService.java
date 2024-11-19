package ec.gob.conagopare.sona.modules.chatbot.service;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.dialogflow.cx.v3.*;
import ec.gob.conagopare.sona.application.common.functions.ConsumerThrowable;
import ec.gob.conagopare.sona.modules.chatbot.ChaBotConfig;
import io.github.luidmidev.springframework.web.problemdetails.ApiError;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;


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

    public SseEmitter sendMessage(String message, Jwt jwt) {
        var emitter = new SseEmitter();
        var session = jwt.getSubject();

        emitter.onCompletion(() -> activeIntentsDetection.remove(session));
        emitter.onTimeout(() -> activeIntentsDetection.remove(session));
        emitter.onError(e -> activeIntentsDetection.remove(session));

        if (activeIntentsDetection.add(session)) {
            CompletableFuture.runAsync(() -> {
                try {
                    asyncStreamingDetectIntent(session, message, response -> {
                        System.out.println(response);
                        emitter.send(response);
                    });
                    emitter.complete();
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            });
        } else {
            emitter.completeWithError(ApiError.conflict("Ya se esta procesando una solicitud"));
        }

        return emitter;
    }

    public <T extends Exception> void asyncStreamingDetectIntent(String sessionId, String text, ConsumerThrowable<StreamingDetectIntentResponse, T> consumer) throws IOException, T {
        try (var sessionsClient = getSessionsClient()) {

            var textInput = TextInput.newBuilder().setText(text);
            var queryInput = QueryInput.newBuilder().setText(textInput).build();

            var bidiStream = sessionsClient.streamingDetectIntentCallable().call();
            var request = StreamingDetectIntentRequest.newBuilder()
                    .setSession(getSessionName(sessionId).toString())
                    .setQueryParams(QueryParameters.newBuilder().build())
                    .setQueryInput(queryInput)
                    .setOutputAudioConfig(OutputAudioConfig.newBuilder().build())
                    .setEnablePartialResponse(true)
                    .setEnableDebuggingInfo(true)
                    .build();

            bidiStream.send(request);

            for (var response : bidiStream) {
                consumer.accept(response);
            }
        }
    }

    public List<ResponseMessage> detectIntent(String sessionId, String text) throws IOException {
        try (var sessionsClient = getSessionsClient()) {

            var textInput = TextInput.newBuilder().setText(text);
            var queryInput = QueryInput.newBuilder().setText(textInput).build();

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
