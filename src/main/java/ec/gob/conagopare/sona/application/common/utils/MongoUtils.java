package ec.gob.conagopare.sona.application.common.utils;

import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public final class MongoUtils {

    private MongoUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static List<Document> toDocuments(String... documents) {
        var parsedDocuments = new ArrayList<Document>();
        for (var document : documents) {
            parsedDocuments.add(Document.parse(document));
        }
        return parsedDocuments;
    }
}