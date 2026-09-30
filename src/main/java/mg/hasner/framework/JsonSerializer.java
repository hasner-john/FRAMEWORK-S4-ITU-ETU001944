package mg.hasner.framework;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Convertit les objets Java simples en JSON sans imposer de bibliotheque externe.
 */
public final class JsonSerializer {

    /**
     * Classe utilitaire : aucune instance n'est necessaire.
     */
    private JsonSerializer() {
    }

    /**
     * Transforme une valeur Java en chaine JSON.
     */
    public static String toJson(Object value) {
        StringBuilder json = new StringBuilder();
        writeValue(json, value, new IdentityHashMap<Object, Boolean>());
        return json.toString();
    }

    /**
     * Ecrit la representation JSON d'une valeur dans le buffer fourni.
     */
    private static void writeValue(StringBuilder json, Object value,
            IdentityHashMap<Object, Boolean> visited) {
        if (value == null) {
            json.append("null");
        } else if (value instanceof String || value instanceof Character || value instanceof Enum<?>) {
            writeString(json, String.valueOf(value));
        } else if (value instanceof Number || value instanceof Boolean) {
            json.append(value);
        } else if (value instanceof Map<?, ?>) {
            writeMap(json, (Map<?, ?>) value, visited);
        } else if (value instanceof Iterable<?>) {
            writeIterable(json, (Iterable<?>) value, visited);
        } else if (value.getClass().isArray()) {
            writeArray(json, value, visited);
        } else {
            writeObject(json, value, visited);
        }
    }

    /**
     * Ecrit une collection Iterable sous forme de tableau JSON.
     */
    private static void writeIterable(StringBuilder json, Iterable<?> values,
            IdentityHashMap<Object, Boolean> visited) {
        verifyNotCircular(values, visited);
        json.append('[');
        Iterator<?> iterator = values.iterator();
        while (iterator.hasNext()) {
            writeValue(json, iterator.next(), visited);
            if (iterator.hasNext()) {
                json.append(',');
            }
        }
        json.append(']');
        visited.remove(values);
    }

    /**
     * Ecrit un tableau Java sous forme de tableau JSON.
     */
    private static void writeArray(StringBuilder json, Object values,
            IdentityHashMap<Object, Boolean> visited) {
        verifyNotCircular(values, visited);
        json.append('[');
        int length = Array.getLength(values);
        for (int index = 0; index < length; index++) {
            writeValue(json, Array.get(values, index), visited);
            if (index < length - 1) {
                json.append(',');
            }
        }
        json.append(']');
        visited.remove(values);
    }

    /**
     * Ecrit une Map sous forme d'objet JSON.
     */
    private static void writeMap(StringBuilder json, Map<?, ?> values,
            IdentityHashMap<Object, Boolean> visited) {
        verifyNotCircular(values, visited);
        json.append('{');
        Iterator<? extends Map.Entry<?, ?>> iterator = values.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<?, ?> entry = iterator.next();
            writeString(json, String.valueOf(entry.getKey()));
            json.append(':');
            writeValue(json, entry.getValue(), visited);
            if (iterator.hasNext()) {
                json.append(',');
            }
        }
        json.append('}');
        visited.remove(values);
    }

    /**
     * Ecrit les champs non statiques d'un objet comme proprietes JSON.
     */
    private static void writeObject(StringBuilder json, Object value,
            IdentityHashMap<Object, Boolean> visited) {
        verifyNotCircular(value, visited);
        json.append('{');
        Field[] fields = value.getClass().getDeclaredFields();
        boolean firstField = true;

        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                continue;
            }

            if (!firstField) {
                json.append(',');
            }
            firstField = false;
            writeString(json, field.getName());
            json.append(':');

            try {
                field.setAccessible(true);
                writeValue(json, field.get(value), visited);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Impossible de lire le champ " + field.getName(), e);
            }
        }

        json.append('}');
        visited.remove(value);
    }

    /**
     * Refuse les objets qui se referencent eux-memes, impossibles a representer en JSON.
     */
    private static void verifyNotCircular(Object value, IdentityHashMap<Object, Boolean> visited) {
        if (visited.put(value, Boolean.TRUE) != null) {
            throw new IllegalArgumentException("Reference circulaire detectee pendant la conversion JSON.");
        }
    }

    /**
     * Ecrit une chaine JSON en echappant ses caracteres speciaux.
     */
    private static void writeString(StringBuilder json, String value) {
        json.append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"':
                    json.append("\\\"");
                    break;
                case '\\':
                    json.append("\\\\");
                    break;
                case '\b':
                    json.append("\\b");
                    break;
                case '\f':
                    json.append("\\f");
                    break;
                case '\n':
                    json.append("\\n");
                    break;
                case '\r':
                    json.append("\\r");
                    break;
                case '\t':
                    json.append("\\t");
                    break;
                default:
                    if (character < 0x20) {
                        json.append(String.format("\\u%04x", (int) character));
                    } else {
                        json.append(character);
                    }
            }
        }
        json.append('"');
    }
}
