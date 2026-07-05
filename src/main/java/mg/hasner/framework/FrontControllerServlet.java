package mg.hasner.framework;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class FrontControllerServlet extends HttpServlet {

    private MappingRegistry registry;

    @Override
    public void init() throws ServletException {
        super.init();
        Object value = getServletContext().getAttribute(FrameworkContextListener.REGISTRY_ATTRIBUTE);

        if (value instanceof MappingRegistry) {
            registry = (MappingRegistry) value;
        } else {
            registry = new FrameworkInitializer(getServletContext()).init();
            getServletContext().setAttribute(FrameworkContextListener.REGISTRY_ATTRIBUTE, registry);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        processRequest(req, res, HttpMethod.GET);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        processRequest(req, res, HttpMethod.POST);
    }

    private void processRequest(HttpServletRequest req, HttpServletResponse res, HttpMethod httpMethod)
            throws ServletException, IOException {
        String url = getRequestedUrl(req);

        res.setContentType("text/html;charset=UTF-8");
        PrintWriter out = res.getWriter();

        out.println("<h2>URL demandee : " + url + " (" + httpMethod + ")</h2>");

        if ("/".equals(url)) {
            output(out);
            return;
        }

        RouteMapping route = registry.findRoute(url, httpMethod);

        if (route == null) {
            throw new ServletException("URL/Methode non reconnue : " + httpMethod + " " + url
                    + ". URLs existantes : " + registry.getExistingUrls());
        }

        out.println("<h3>Mapping trouve</h3>");
        out.println("<p>Controller : <b>" + route.getControllerClass().getSimpleName() + "</b></p>");
        out.println("<p>Methode : <b>" + route.getMethod().getName() + "()</b></p>");
        out.println("<p>HTTP : <b>" + httpMethod + "</b></p>");
        out.println("<h3>Resultat</h3>");
        out.println("<p>" + invokeRoute(route) + "</p>");
    }

    private Object invokeRoute(RouteMapping route) throws ServletException {
        try {
            Object result = route.invoke();
            return result == null ? "Methode invoquee avec succes." : result;
        } catch (ReflectiveOperationException e) {
            throw new ServletException("Erreur pendant l'invocation de la methode "
                    + route.getMethod().getName(), e);
        }
    }

    private void output(PrintWriter out) {
        out.println("<h3>Controleurs detectes</h3>");

        if (registry.getControllers().isEmpty()) {
            out.println("<p>Aucun controleur trouve.</p>");
            return;
        }

        out.println("<ul>");
        for (Class<?> clazz : registry.getControllers()) {
            out.println("<li><b>" + clazz.getSimpleName() + "</b>");

            out.println("<ul>");
            boolean hasMappedMethod = false;
            for (RouteMapping route : registry.getRoutes().values()) {
                if (route.getControllerClass().equals(clazz)) {
                    hasMappedMethod = true;
                    out.println("<li>[" + route.getHttpMethod() + "] " + route.getUrl()
                            + " -&gt; " + route.getMethod().getName() + "()</li>");
                }
            }

            if (!hasMappedMethod) {
                out.println("<li>Aucune methode annotee @UrlMapping</li>");
            }

            out.println("</ul>");
            out.println("</li>");
        }
        out.println("</ul>");
    }

    private String getRequestedUrl(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();

        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }

        return uri.isEmpty() ? "/" : uri;
    }
}
