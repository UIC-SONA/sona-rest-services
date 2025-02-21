package ec.gob.conagopare.sona.test;

import org.mockito.stubbing.Answer;

public final class StorageTestUtils {

    private StorageTestUtils() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    public static final Answer<String> STORAGE_SAVE_ANSWER = invocation -> {
        var name = invocation.getArgument(1, String.class);
        var path = invocation.getArgument(2, String.class);
        return path + "/" + name;
    };


}
