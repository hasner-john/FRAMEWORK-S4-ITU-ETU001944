package mg.hasner.framework;

import java.util.HashMap;
import java.util.Map;

public class ModelAndView {
    private String view;
    private final Map<String, Object> data;

    /**
     * Cree un modele de reponse avec le nom logique de la vue a afficher.
     */
    public ModelAndView(String view) {
        this.view = view;
        this.data = new HashMap<>();
    }

    /**
     * Retourne le nom logique de la vue.
     */
    public String getView() {
        return view;
    }

    /**
     * Modifie le nom logique de la vue.
     */
    public void setView(String view) {
        this.view = view;
    }

    /**
     * Retourne les donnees qui seront placees dans la requete servlet.
     */
    public Map<String, Object> getData() {
        return data;
    }

    /**
     * Ajoute une donnee au modele avec sa cle d'acces dans la JSP.
     */
    public ModelAndView addAttribute(String key, Object value) {
        data.put(key, value);
        return this;
    }

    /**
     * Alias francise de addAttribute, garde pour correspondre au sujet.
     */
    public ModelAndView addAttribut(String key, Object value) {
        return addAttribute(key, value);
    }
}
