package mg.hasner.framework;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.Map;

public class RouteMapping {
    private final UrlMethode urlMethode;
    private final Class<?> controllerClass;
    private final Method method;

    /**
     * Construit le lien entre une URL/methode HTTP et une methode de controleur.
     */
    public RouteMapping(UrlMethode urlMethode, Class<?> controllerClass, Method method) {
        this.urlMethode = urlMethode;
        this.controllerClass = controllerClass;
        this.method = method;
    }

    /**
     * Retourne la cle complete de route : URL + methode HTTP.
     */
    public UrlMethode getUrlMethode() {
        return urlMethode;
    }

    /**
     * Retourne uniquement l'URL de la route.
     */
    public String getUrl() {
        return urlMethode.getUrl();
    }

    /**
     * Retourne la methode HTTP associee a la route.
     */
    public HttpMethod getHttpMethod() {
        return urlMethode.getMethode();
    }

    /**
     * Retourne la classe du controleur qui contient la methode.
     */
    public Class<?> getControllerClass() {
        return controllerClass;
    }

    /**
     * Retourne la methode Java a invoquer.
     */
    public Method getMethod() {
        return method;
    }

    /**
     * Instancie le controleur et execute la methode cible par reflexion.
     * Si la methode demande un SpringContext, le framework le lui donne.
     */
    public Object invoke(SpringContext springContext) throws ReflectiveOperationException {
        return invoke(springContext, Collections.<String, String[]>emptyMap());
    }

    /**
     * Instancie le controleur et lie les parametres HTTP aux parametres simples de la methode.
     * Un parametre SpringContext est injecte par le framework et ne vient pas de la requete.
     */
    public Object invoke(SpringContext springContext, Map<String, String[]> requestParameters)
            throws ReflectiveOperationException {
        Object controller = controllerClass.getDeclaredConstructor().newInstance();
        method.setAccessible(true);
        Parameter[] methodParameters = method.getParameters();

        if (methodParameters.length == 0) {
            return method.invoke(controller);
        }

        Object[] arguments = new Object[methodParameters.length];
        Map<String, String[]> parameters = requestParameters == null
                ? Collections.<String, String[]>emptyMap() : requestParameters;

        for (int index = 0; index < methodParameters.length; index++) {
            arguments[index] = bindParameter(methodParameters[index], parameters, springContext);
        }

        return method.invoke(controller, arguments);
    }

    /**
     * Associe un parametre de methode a SpringContext ou a la valeur HTTP de meme nom.
     */
    private Object bindParameter(Parameter parameter, Map<String, String[]> requestParameters,
            SpringContext springContext) {
        if (parameter.getType().equals(SpringContext.class)) {
            return springContext;
        }

        if (!parameter.isNamePresent()) {
            throw new IllegalArgumentException("Le nom du parametre de "
                    + controllerClass.getSimpleName() + "." + method.getName()
                    + "() est indisponible. Compilez TEST avec l'option -parameters.");
        }

        String parameterName = parameter.getName();
        String[] values = requestParameters.get(parameterName);

        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Parametre de requete manquant : " + parameterName
                    + " pour " + controllerClass.getSimpleName() + "." + method.getName() + "().");
        }

        return convertValue(parameterName, values[0], parameter.getType());
    }

    /**
     * Convertit une valeur de formulaire vers un type Java simple.
     * Le binding d'objet est reserve au Sprint 7 bis.
     */
    private Object convertValue(String parameterName, String value, Class<?> parameterType) {
        try {
            if (parameterType.equals(String.class)) {
                return value;
            }
            if (parameterType.equals(int.class) || parameterType.equals(Integer.class)) {
                return Integer.valueOf(value);
            }
            if (parameterType.equals(long.class) || parameterType.equals(Long.class)) {
                return Long.valueOf(value);
            }
            if (parameterType.equals(double.class) || parameterType.equals(Double.class)) {
                return Double.valueOf(value);
            }
            if (parameterType.equals(float.class) || parameterType.equals(Float.class)) {
                return Float.valueOf(value);
            }
            if (parameterType.equals(boolean.class) || parameterType.equals(Boolean.class)) {
                return Boolean.valueOf(value);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valeur invalide pour le parametre " + parameterName
                    + " : " + value + ". Type attendu : " + parameterType.getSimpleName() + ".", e);
        }

        throw new IllegalArgumentException("Type non supporte pour le parametre " + parameterName
                + " : " + parameterType.getName()
                + ". Le binding d'objet sera disponible au Sprint 7 bis.");
    }

    /**
     * Garde l'ancien appel sans parametre pour les usages internes simples.
     */
    public Object invoke() throws ReflectiveOperationException {
        Object controller = controllerClass.getDeclaredConstructor().newInstance();
        method.setAccessible(true);
        return method.invoke(controller);
    }
}
