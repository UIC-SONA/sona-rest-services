package ec.gob.conagopare.sona.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnvironmentChecker {

    private final Environment environment;

    public boolean isProduction() {
        for (String profile : environment.getActiveProfiles()) {
            if (profile.equalsIgnoreCase("prod")) return true;
        }
        return false;
    }

    public boolean isDevelopment() {
        return !isProduction();
    }
}
