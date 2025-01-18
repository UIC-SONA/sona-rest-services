package ec.gob.conagopare.sona.application.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        try (var resourceCredentials = new FileInputStream("google/service_account_firebase_adminsdk.json")) {
            var options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(resourceCredentials))
                    .build();

            FirebaseApp.initializeApp(options);
            var instance = FirebaseApp.getInstance();
            log.info("Firebase app initialized");
            return instance;
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
