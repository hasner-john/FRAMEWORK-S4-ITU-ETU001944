package mg.hasner.framework;

import java.lang.reflect.Method;

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
     */
    public Object invoke() throws ReflectiveOperationException {
        Object controller = controllerClass.getDeclaredConstructor().newInstance();
        method.setAccessible(true);
        return method.invoke(controller);
    }
}
