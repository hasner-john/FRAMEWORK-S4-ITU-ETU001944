package mg.hasner.framework;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MappingRegistry {
    private final List<Class<?>> controllers;
    private final Map<UrlMethode, RouteMapping> routes;

    public MappingRegistry(List<Class<?>> controllers, Map<UrlMethode, RouteMapping> routes) {
        this.controllers = controllers;
        this.routes = routes;
    }

    public List<Class<?>> getControllers() {
        return Collections.unmodifiableList(controllers);
    }

    public Map<UrlMethode, RouteMapping> getRoutes() {
        return Collections.unmodifiableMap(routes);
    }

    public RouteMapping findRoute(String url, HttpMethod httpMethod) {
        return routes.get(new UrlMethode(url, httpMethod));
    }

    public String getExistingUrls() {
        if (routes == null || routes.isEmpty()) {
            return "aucune URL disponible";
        }

        StringBuilder builder = new StringBuilder();
        for (UrlMethode key : routes.keySet()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(key);
        }

        return builder.toString();
    }
}
