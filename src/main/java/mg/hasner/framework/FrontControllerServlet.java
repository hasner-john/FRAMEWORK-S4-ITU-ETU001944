package mg.hasner.framework;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class FrontControllerServlet extends HttpServlet {

    private MappingRegistry registry;
    private SpringContext springContext;
    private String viewPrefix;
    private String viewSuffix;

    /**
     * Initialise le servlet et recupere le mapping deja construit par le listener.
     * Si le listener n'a pas encore prepare le registre, le servlet le cree lui-meme.
     */
    @Override
    public void init() throws ServletException {
        super.init();
        viewPrefix = getConfiguredValue("viewPrefix", "/");
        viewSuffix = getConfiguredValue("viewSuffix", ".jsp");

        Object value = getServletContext().getAttribute(FrameworkContextListener.REGISTRY_ATTRIBUTE);

        if (value instanceof MappingRegistry) {
            registry = (MappingRegistry) value;
        } else {
            registry = new FrameworkInitializer(getServletContext()).init();
            getServletContext().setAttribute(FrameworkContextListener.REGISTRY_ATTRIBUTE, registry);
        }

        Object springValue = getServletContext().getAttribute(FrameworkContextListener.SPRING_CONTEXT_ATTRIBUTE);
        if (springValue instanceof SpringContext) {
            springContext = (SpringContext) springValue;
        } else {
            springContext = SpringContext.fromServletContext(getServletContext());
            getServletContext().setAttribute(FrameworkContextListener.SPRING_CONTEXT_ATTRIBUTE, springContext);
        }
    }

    /**
     * Traite les requetes HTTP GET en gardant la methode HTTP dans la cle de route.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        processRequest(req, res, HttpMethod.GET);
    }

    /**
     * Traite les requetes HTTP POST en gardant la methode HTTP dans la cle de route.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        processRequest(req, res, HttpMethod.POST);
    }

    /**
     * Cherche la route correspondant a l'URL et a la methode HTTP, puis dispatch vers la vue.
     */
    private void processRequest(HttpServletRequest req, HttpServletResponse res, HttpMethod httpMethod)
            throws ServletException, IOException {
        String url = getRequestedUrl(req);

        res.setContentType("text/html;charset=UTF-8");

        if ("/".equals(url)) {
            PrintWriter out = res.getWriter();
            out.println("<h2>URL demandee : " + url + " (" + httpMethod + ")</h2>");
            output(out);
            return;
        }

        RouteMapping route = registry.findRoute(url, httpMethod);

        if (route == null) {
            throw new ServletException("URL/Methode non reconnue : " + httpMethod + " " + url
                    + ". URLs existantes : " + registry.getExistingUrls());
        }

        dispatchModelAndView(req, res, route);
    }

    /**
     * Invoque la methode du controleur, exige un ModelAndView et fait le forward vers la JSP.
     */
    private void dispatchModelAndView(HttpServletRequest req, HttpServletResponse res, RouteMapping route)
            throws ServletException, IOException {
        Object result = invokeRoute(route);

        if (!(result instanceof ModelAndView)) {
            throw new ServletException("Type de retour invalide pour "
                    + route.getControllerClass().getSimpleName() + "." + route.getMethod().getName()
                    + "() : ModelAndView attendu.");
        }

        ModelAndView modelAndView = (ModelAndView) result;
        addArgToRequest(req, modelAndView.getData());

        RequestDispatcher dispatcher = req.getRequestDispatcher(getViewPath(modelAndView.getView()));
        dispatcher.forward(req, res);
    }

    /**
     * Execute la methode Java associee a la route par reflexion.
     */
    private Object invokeRoute(RouteMapping route) throws ServletException {
        try {
            return route.invoke(springContext);
        } catch (ReflectiveOperationException e) {
            throw new ServletException("Erreur pendant l'invocation de la methode "
                    + route.getMethod().getName(), e);
        } catch (RuntimeException e) {
            throw new ServletException("Erreur pendant l'invocation de la methode "
                    + route.getMethod().getName(), e);
        }
    }

    /**
     * Copie toutes les donnees du ModelAndView dans la requete servlet.
     */
    private void addArgToRequest(HttpServletRequest req, Map<String, Object> data) {
        if (data == null) {
            return;
        }

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            req.setAttribute(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Transforme le nom logique de vue en chemin JSP avec prefixe et suffixe.
     * Exemple : "list" devient "/list.jsp" avec les valeurs par defaut.
     */
    private String getViewPath(String view) throws ServletException {
        if (view == null || view.trim().isEmpty()) {
            throw new ServletException("La vue du ModelAndView est vide.");
        }

        String viewName = view.trim();
        String path = viewName.startsWith("/") ? viewName : viewPrefix + viewName;

        if (!viewSuffix.isEmpty() && !path.endsWith(viewSuffix)) {
            path += viewSuffix;
        }

        return path;
    }

    /**
     * Affiche la liste des controleurs detectes et leurs mappings.
     */
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

    /**
     * Retire le context path de Tomcat pour obtenir seulement l'URL applicative.
     */
    private String getRequestedUrl(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();

        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }

        return uri.isEmpty() ? "/" : uri;
    }

    /**
     * Lit une configuration du servlet dans web.xml et utilise une valeur par defaut si elle manque.
     */
    private String getConfiguredValue(String name, String defaultValue) {
        String value = getServletConfig().getInitParameter(name);
        return value == null ? defaultValue : value;
    }
}
