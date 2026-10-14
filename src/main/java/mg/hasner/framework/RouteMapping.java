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
     * Associe un parametre de methode a SpringContext ou aux donnees HTTP.
     */
    private Object bindParameter(Parameter parameter, Map<String, String[]> requestParameters,
            SpringContext springContext) {
        if (parameter.getType().equals(SpringContext.class)) {
            return springContext;
        }

        return ObjectBinder.bind(parameter, requestParameters);
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
