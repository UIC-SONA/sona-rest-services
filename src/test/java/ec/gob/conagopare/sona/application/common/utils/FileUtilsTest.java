package ec.gob.conagopare.sona.application.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilsTest {

    @Test
    void testConstructor() {
        assertThrows(UnsupportedOperationException.class, FileUtils::new);
    }

}