package mg.hasner.framework;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Conserve une MaSession par client dans le ServletContext de l'application.
 */
public final class SessionRegistry {
    /** Cle utilisee pour stocker le registre dans ServletContext. */
    public static final String ATTRIBUTE_NAME = "mg.hasner.framework.sessionRegistry";

    /**
     * Classe utilitaire : aucune instance n'est necessaire.
     */
    private SessionRegistry() {
    }

    /**
     * Initialise le registre de sessions lors du demarrage de l'application.
     */
    public static void initialize(ServletContext servletContext) {
        servletContext.setAttribute(ATTRIBUTE_NAME, new ConcurrentHashMap<String, MaSession>());
    }

    /**
     * Retourne la session applicative unique associee au client HTTP.
     */
    public static MaSession getOrCreate(ServletContext servletContext, HttpSession httpSession) {
        Map<String, MaSession> sessions = getSessions(servletContext);
        return sessions.computeIfAbsent(httpSession.getId(), ignored -> MaSession.from(httpSession));
    }

    /**
     * Supprime la session applicative quand Tomcat detruit la session HTTP du client.
     */
    public static void remove(ServletContext servletContext, String sessionId) {
        getSessions(servletContext).remove(sessionId);
    }

    /**
     * Vide le registre a l'arret de l'application pour liberer la memoire.
     */
    public static void clear(ServletContext servletContext) {
        getSessions(servletContext).clear();
        servletContext.removeAttribute(ATTRIBUTE_NAME);
    }

    /**
     * Lit le registre ou le recree si le listener n'a pas encore ete appele.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, MaSession> getSessions(ServletContext servletContext) {
        Object value = servletContext.getAttribute(ATTRIBUTE_NAME);
        if (value instanceof Map<?, ?>) {
            return (Map<String, MaSession>) value;
        }

        Map<String, MaSession> sessions = new ConcurrentHashMap<>();
        servletContext.setAttribute(ATTRIBUTE_NAME, sessions);
        return sessions;
    }
}
