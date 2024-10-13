package ec.gob.conagopare.sona;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.ketoru",
        "ec.gob.conagopare.sona"
})
public class SonaRestServicesApplication {

    public static void main(String[] args) {
        SpringApplication.run(SonaRestServicesApplication.class, args);
    }

}
