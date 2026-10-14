package mg.hasner.framework;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Lie les parametres HTTP aux types simples et aux objets demandes par un controleur.
 */
public final class ObjectBinder {

    /**
     * Classe utilitaire : aucune instance n'est necessaire.
     */
    private ObjectBinder() {
    }

    /**
     * Cree la valeur correspondant a un parametre de methode de controleur.
     */
    public static Object bind(Parameter parameter, Map<String, String[]> requestParameters) {
        if (!parameter.isNamePresent()) {
            throw new IllegalArgumentException("Le nom du parametre " + parameter.getName()
                    + " est indisponible. Compilez TEST avec l'option -parameters.");
        }

        Map<String, String[]> parameters = requestParameters == null
                ? Collections.<String, String[]>emptyMap() : requestParameters;
        Class<?> parameterType = parameter.getType();

        if (isSimpleType(parameterType)) {
            String value = getRequiredValue(parameter.getName(), parameters);
            return convertValue(parameter.getName(), value, parameterType);
        }

        return bindObject(parameterType, rootPrefixes(parameter.getName()), parameters);
    }

    /**
     * Instancie un objet puis renseigne recursivement ses champs a partir de la requete.
     */
    private static Object bindObject(Class<?> objectType, List<String> prefixes,
            Map<String, String[]> requestParameters) {
        Object object = instantiate(objectType);

        for (Field field : getBindableFields(objectType)) {
            List<String> fieldPaths = appendField(prefixes, field.getName());
            Object value = getFieldValue(field, fieldPaths, requestParameters);

            if (value != null) {
                setField(field, object, value);
            }
        }

        return object;
    }

    /**
     * Retourne la valeur liee a un champ simple, objet ou liste d'objets.
     */
    private static Object getFieldValue(Field field, List<String> fieldPaths,
            Map<String, String[]> requestParameters) {
        Class<?> fieldType = field.getType();

        if (isSimpleType(fieldType)) {
            String value = getOptionalValue(fieldPaths, requestParameters);
            return value == null ? null : convertValue(field.getName(), value, fieldType);
        }

        if (List.class.isAssignableFrom(fieldType)) {
            return bindList(field, fieldPaths, requestParameters);
        }

        if (hasNestedValue(fieldPaths, requestParameters)) {
            return bindObject(fieldType, fieldPaths, requestParameters);
        }

        return null;
    }

    /**
     * Cree une liste en utilisant des noms de champs du type competences[0].nom.
     */
    private static List<Object> bindList(Field field, List<String> listPaths,
            Map<String, String[]> requestParameters) {
        Class<?> elementType = getListElementType(field);
        Set<Integer> indexes = getIndexes(listPaths, requestParameters);

        if (indexes.isEmpty()) {
            return null;
        }

        List<Object> result = new ArrayList<>();
        for (Integer index : indexes) {
            List<String> elementPaths = appendIndex(listPaths, index.intValue());
            if (isSimpleType(elementType)) {
                String value = getOptionalValue(elementPaths, requestParameters);
                result.add(value == null ? null : convertValue(field.getName(), value, elementType));
            } else {
                result.add(bindObject(elementType, elementPaths, requestParameters));
            }
        }

        return result;
    }

    /**
     * Recupere le type genere par une declaration List<T>.
     */
    private static Class<?> getListElementType(Field field) {
        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType)) {
            throw new IllegalArgumentException("La liste " + field.getName()
                    + " doit declarer son type, par exemple List<Skill>.");
        }

        Type elementType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
        if (elementType instanceof Class<?>) {
            return (Class<?>) elementType;
        }

        throw new IllegalArgumentException("Type de liste non supporte pour " + field.getName() + ".");
    }

    /**
     * Extrait les index presents dans les noms de parametres de la requete.
     */
    private static Set<Integer> getIndexes(List<String> listPaths,
            Map<String, String[]> requestParameters) {
        Set<Integer> indexes = new TreeSet<>();

        for (String parameterName : requestParameters.keySet()) {
            for (String listPath : listPaths) {
                String prefix = listPath + "[";
                if (!parameterName.startsWith(prefix)) {
                    continue;
                }

                int closingBracket = parameterName.indexOf(']', prefix.length());
                if (closingBracket < 0) {
                    continue;
                }

                try {
                    indexes.add(Integer.valueOf(parameterName.substring(prefix.length(), closingBracket)));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Index de liste invalide dans " + parameterName + ".", e);
                }
            }
        }

        return indexes;
    }

    /**
     * Retourne true lorsqu'au moins un champ enfant est present dans la requete.
     */
    private static boolean hasNestedValue(List<String> paths, Map<String, String[]> requestParameters) {
        for (String parameterName : requestParameters.keySet()) {
            for (String path : paths) {
                if (parameterName.startsWith(path + ".") || parameterName.startsWith(path + "[")) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Retourne les champs d'instance, y compris ceux declares par une classe parente.
     */
    private static List<Field> getBindableFields(Class<?> objectType) {
        List<Field> fields = new ArrayList<>();
        Class<?> currentType = objectType;

        while (currentType != null && !currentType.equals(Object.class)) {
            for (Field field : currentType.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (!Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers) && !field.isSynthetic()) {
                    fields.add(field);
                }
            }
            currentType = currentType.getSuperclass();
        }

        return fields;
    }

    /**
     * Cree une instance avec le constructeur sans argument requis pour le binding.
     */
    private static Object instantiate(Class<?> objectType) {
        if (objectType.isInterface() || Modifier.isAbstract(objectType.getModifiers())) {
            throw new IllegalArgumentException("Le type " + objectType.getName()
                    + " ne peut pas etre instancie pour le binding.");
        }

        try {
            Constructor<?> constructor = objectType.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Le type " + objectType.getName()
                    + " doit posseder un constructeur sans argument pour le binding.", e);
        }
    }

    /**
     * Affecte une valeur a un champ prive ou public de l'objet en cours de construction.
     */
    private static void setField(Field field, Object object, Object value) {
        try {
            field.setAccessible(true);
            field.set(object, value);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Impossible de renseigner le champ " + field.getName() + ".", e);
        }
    }

    /**
     * Retourne les deux conventions acceptees a la racine : employee.nom et nom.
     */
    private static List<String> rootPrefixes(String parameterName) {
        List<String> prefixes = new ArrayList<>();
        prefixes.add(parameterName);
        prefixes.add("");
        return prefixes;
    }

    /**
     * Ajoute un nom de champ a chaque chemin de binding candidat.
     */
    private static List<String> appendField(List<String> prefixes, String fieldName) {
        List<String> paths = new ArrayList<>();
        for (String prefix : prefixes) {
            paths.add(prefix.isEmpty() ? fieldName : prefix + "." + fieldName);
        }
        return paths;
    }

    /**
     * Ajoute un index de liste a chaque chemin de binding candidat.
     */
    private static List<String> appendIndex(List<String> prefixes, int index) {
        List<String> paths = new ArrayList<>();
        for (String prefix : prefixes) {
            paths.add(prefix + "[" + index + "]");
        }
        return paths;
    }

    /**
     * Retourne une valeur obligatoire pour un parametre simple de methode.
     */
    private static String getRequiredValue(String parameterName, Map<String, String[]> requestParameters) {
        String value = getOptionalValue(Collections.singletonList(parameterName), requestParameters);
        if (value == null) {
            throw new IllegalArgumentException("Parametre de requete manquant : " + parameterName + ".");
        }
        return value;
    }

    /**
     * Retourne la premiere valeur trouvee parmi les chemins candidats.
     */
    private static String getOptionalValue(List<String> parameterNames,
            Map<String, String[]> requestParameters) {
        for (String parameterName : parameterNames) {
            String[] values = requestParameters.get(parameterName);
            if (values != null && values.length > 0) {
                return values[0];
            }
        }
        return null;
    }

    /**
     * Indique si le type peut etre construit depuis une seule valeur de formulaire.
     */
    private static boolean isSimpleType(Class<?> type) {
        return type.equals(String.class)
                || type.equals(int.class) || type.equals(Integer.class)
                || type.equals(long.class) || type.equals(Long.class)
                || type.equals(double.class) || type.equals(Double.class)
                || type.equals(float.class) || type.equals(Float.class)
                || type.equals(boolean.class) || type.equals(Boolean.class)
                || type.equals(short.class) || type.equals(Short.class)
                || type.equals(byte.class) || type.equals(Byte.class)
                || type.equals(char.class) || type.equals(Character.class)
                || type.isEnum();
    }

    /**
     * Convertit une chaine provenant du formulaire vers un type Java simple.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Object convertValue(String parameterName, String value, Class<?> targetType) {
        try {
            if (targetType.equals(String.class)) {
                return value;
            }
            if (targetType.equals(int.class) || targetType.equals(Integer.class)) {
                return Integer.valueOf(value);
            }
            if (targetType.equals(long.class) || targetType.equals(Long.class)) {
                return Long.valueOf(value);
            }
            if (targetType.equals(double.class) || targetType.equals(Double.class)) {
                return Double.valueOf(value);
            }
            if (targetType.equals(float.class) || targetType.equals(Float.class)) {
                return Float.valueOf(value);
            }
            if (targetType.equals(short.class) || targetType.equals(Short.class)) {
                return Short.valueOf(value);
            }
            if (targetType.equals(byte.class) || targetType.equals(Byte.class)) {
                return Byte.valueOf(value);
            }
            if (targetType.equals(boolean.class) || targetType.equals(Boolean.class)) {
                if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                    throw new IllegalArgumentException("Valeur booleenne invalide : " + value + ".");
                }
                return Boolean.valueOf(value);
            }
            if (targetType.equals(char.class) || targetType.equals(Character.class)) {
                if (value.length() != 1) {
                    throw new IllegalArgumentException("Une seule lettre est attendue.");
                }
                return Character.valueOf(value.charAt(0));
            }
            if (targetType.isEnum()) {
                return Enum.valueOf((Class<? extends Enum>) targetType, value);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valeur invalide pour le champ " + parameterName
                    + " : " + value + ". Type attendu : " + targetType.getSimpleName() + ".", e);
        }

        throw new IllegalArgumentException("Type non supporte pour le champ " + parameterName
                + " : " + targetType.getName() + ".");
    }
}
