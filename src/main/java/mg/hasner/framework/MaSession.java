package mg.hasner.framework;

import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session simplifiee exposee aux controleurs sans dependre directement de HttpSession.
 */
public class MaSession {
    private final String id;
    private final Map<String, Object> attributes;
    private final Set<String> initialKeys;

    /**
     * Cree une session vide associee a un identifiant de session HTTP.
     */
    private MaSession(String id) {
        this.id = id;
        this.attributes = new ConcurrentHashMap<>();
        this.initialKeys = ConcurrentHashMap.newKeySet();
    }

    /**
     * Copie l'etat initial d'une HttpSession dans une MaSession.
     */
    public static MaSession from(HttpSession httpSession) {
        if (httpSession == null) {
            throw new IllegalArgumentException("La session HTTP est obligatoire.");
        }

        MaSession session = new MaSession(httpSession.getId());
        Enumeration<String> names = httpSession.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            Object value = httpSession.getAttribute(name);
            if (value != null) {
                session.attributes.put(name, value);
                session.initialKeys.add(name);
            }
        }
        return session;
    }

    /**
     * Retourne l'identifiant stable de la session du client.
     */
    public String getId() {
        return id;
    }

    /**
     * Retourne un attribut stocke dans la session applicative.
     */
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     * Ajoute ou remplace un attribut. Une valeur null equivaut a une suppression.
     */
    public void setAttribute(String name, Object value) {
        validateName(name);
        if (value == null) {
            attributes.remove(name);
            return;
        }
        attributes.put(name, value);
    }

    /**
     * Supprime un attribut de la session applicative.
     */
    public void removeAttribute(String name) {
        validateName(name);
        attributes.remove(name);
    }

    /**
     * Retourne une vue non modifiable des attributs pour lecture uniquement.
     */
    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    /**
     * Recopie les suppressions et les modifications vers la vraie HttpSession.
     */
    public void flushTo(HttpSession httpSession) {
        if (httpSession == null) {
            return;
        }

        for (String key : initialKeys) {
            if (!attributes.containsKey(key)) {
                httpSession.removeAttribute(key);
            }
        }

        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            httpSession.setAttribute(entry.getKey(), entry.getValue());
        }

        initialKeys.clear();
        initialKeys.addAll(attributes.keySet());
    }

    /**
     * Verifie que la cle utilisee par le developpeur est exploitable.
     */
    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom d'attribut de session est obligatoire.");
        }
    }
}
