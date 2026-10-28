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
        return invoke(springContext, requestParameters, Collections.<String, MaFile>emptyMap(), null);
    }

    /**
     * Instancie le controleur et lie les parametres HTTP, SpringContext et les fichiers uploades.
     */
    public Object invoke(SpringContext springContext, Map<String, String[]> requestParameters,
            Map<String, MaFile> uploadedFiles) throws ReflectiveOperationException {
        return invoke(springContext, requestParameters, uploadedFiles, null);
    }

    /**
     * Instancie le controleur et lie les parametres HTTP, fichiers et session applicative.
     */
    public Object invoke(SpringContext springContext, Map<String, String[]> requestParameters,
            Map<String, MaFile> uploadedFiles, MaSession session) throws ReflectiveOperationException {
        Object controller = controllerClass.getDeclaredConstructor().newInstance();
        method.setAccessible(true);
        Parameter[] methodParameters = method.getParameters();

        if (methodParameters.length == 0) {
            return method.invoke(controller);
        }

        Object[] arguments = new Object[methodParameters.length];
        Map<String, String[]> parameters = requestParameters == null
                ? Collections.<String, String[]>emptyMap() : requestParameters;
        Map<String, MaFile> files = uploadedFiles == null
                ? Collections.<String, MaFile>emptyMap() : uploadedFiles;

        for (int index = 0; index < methodParameters.length; index++) {
            arguments[index] = bindParameter(methodParameters[index], parameters, files, springContext, session);
        }

        return method.invoke(controller, arguments);
    }

    /**
     * Associe un parametre de methode a SpringContext ou aux donnees HTTP.
     */
    private Object bindParameter(Parameter parameter, Map<String, String[]> requestParameters,
            Map<String, MaFile> uploadedFiles, SpringContext springContext, MaSession session) {
        if (parameter.getType().equals(SpringContext.class)) {
            return springContext;
        }

        if (parameter.getType().equals(MaSession.class)) {
            if (session == null) {
                throw new IllegalArgumentException("La MaSession n'a pas ete preparee pour cette route.");
            }
            return session;
        }

        if (parameter.getType().equals(MaFile.class)) {
            if (!parameter.isNamePresent()) {
                throw new IllegalArgumentException("Le nom du parametre fichier est indisponible."
                        + " Compilez TEST avec l'option -parameters.");
            }

            MaFile uploadedFile = uploadedFiles.get(parameter.getName());
            if (uploadedFile == null) {
                throw new IllegalArgumentException("Fichier manquant pour le parametre "
                        + parameter.getName() + ".");
            }
            return uploadedFile;
        }

        return ObjectBinder.bind(parameter, requestParameters);
    }

    /**
     * Indique si la methode de controleur demande la session applicative.
     */
    public boolean needsSession() {
        for (Parameter parameter : method.getParameters()) {
            if (parameter.getType().equals(MaSession.class)) {
                return true;
            }
        }
        return false;
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
