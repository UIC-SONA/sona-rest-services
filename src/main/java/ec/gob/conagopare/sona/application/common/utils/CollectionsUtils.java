package ec.gob.conagopare.sona.application.common.utils;

import java.util.Collection;
import java.util.HashSet;

public final class CollectionsUtils {

    CollectionsUtils() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Fusiona dos colecciones, eliminando los elementos no presentes en la nueva colección
     * y agregando los elementos que no están en la colección actual.
     *
     * @param collection la colección actual
     * @param newCollection la nueva colección con la cual se quiere fusionar
     * @param <T> el tipo de los elementos en las colecciones
     */
    public static <T> void merge(Collection<T> collection, Collection<T> newCollection) {
        // Identificar los elementos a eliminar (en currentCollection pero no en newCollection)
        var toRemove = new HashSet<>(collection);
        toRemove.removeAll(newCollection);

        // Identificar los elementos a agregar (en newCollection pero no en currentCollection)
        var toAdd = new HashSet<>(newCollection);
        toAdd.removeAll(collection);

        collection.removeAll(toRemove);
        collection.addAll(toAdd);
    }
}
