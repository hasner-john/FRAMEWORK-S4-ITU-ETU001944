package mg.hasner.framework;

import java.util.Objects;

/**
 * Sprint 3 : cle composite utilisee pour identifier une route de maniere unique.
 * Deux routes sont considerees comme identiques si elles ont la meme URL
 * ET la meme methode HTTP (GET ou POST).
 * Cette classe sert de cle dans la Map<UrlMethode, RouteMapping> du servlet.
 */
public class UrlMethode {

    private final String url;
    private final HttpMethod methode;

    /**
     * Cree une cle de route composee de l'URL et de la methode HTTP.
     */
    public UrlMethode(String url, HttpMethod methode) {
        this.url = url;
        this.methode = methode;
    }

    /**
     * Retourne l'URL de la route.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Retourne la methode HTTP associee a la route.
     */
    public HttpMethod getMethode() {
        return methode;
    }

    /**
     * Deux routes sont egales si elles ont la meme URL et la meme methode HTTP.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof UrlMethode)) {
            return false;
        }
        UrlMethode other = (UrlMethode) obj;
        return Objects.equals(this.url, other.url) && this.methode == other.methode;
    }

    /**
     * Calcule un hash coherent avec equals pour utiliser UrlMethode comme cle de Map.
     */
    @Override
    public int hashCode() {
        return Objects.hash(url, methode);
    }

    /**
     * Affiche la route sous forme lisible, par exemple GET /list.
     */
    @Override
    public String toString() {
        return methode + " " + url;
    }
}
