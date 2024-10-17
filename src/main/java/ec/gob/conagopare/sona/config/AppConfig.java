package ec.gob.conagopare.sona.config;

import com.ketoru.store.core.FileStore;
import com.ketoru.store.disk.DiskFileStoreService;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Log4j2
@Configuration
public class AppConfig {

    public static final FileStore.FilenameMutator FILENAME_MUTATOR = (filename, path) -> {
        var uuid = UUID.randomUUID().toString();
        var extension = filename.substring(filename.lastIndexOf('.'));
        var safename = uuid + extension;
        log.info("Saving file {}", safename);
        return safename;
    };

    @Bean
    public FileStore fileStore() {
        var store = new DiskFileStoreService();
        store.setFilenameMutator(FILENAME_MUTATOR);
        return store;
    }
}
