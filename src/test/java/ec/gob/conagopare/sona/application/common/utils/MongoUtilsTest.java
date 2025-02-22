package ec.gob.conagopare.sona.application.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MongoUtilsTest {

    @Test
    void testConstructor() {
        assertThrows(UnsupportedOperationException.class, MongoUtils::new);
    }

}