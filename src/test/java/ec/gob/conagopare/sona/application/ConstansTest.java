package ec.gob.conagopare.sona.application;

import org.junit.Test;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("Unit testing of utilities classes instantiation")
public class ConstansTest {

    @Test
    public void testConstructor() {
        assertThrows(UnsupportedOperationException.class, Constans::new);
    }

}
