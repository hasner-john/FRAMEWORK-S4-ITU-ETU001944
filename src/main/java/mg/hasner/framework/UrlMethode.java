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

    public UrlMethode(String url, HttpMethod methode) {
        this.url = url;
        this.methode = methode;
    }

    public String getUrl() {
        return url;
    }

    public HttpMethod getMethode() {
        return methode;
    }

    // Deux UrlMethode sont egales si meme url ET meme methode HTTP
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

    // hashCode coherent avec equals (obligatoire pour une utilisation correcte en cle de Map)
    @Override
    public int hashCode() {
        return Objects.hash(url, methode);
    }

    @Override
    public String toString() {
        return methode + " " + url;
    }
}
