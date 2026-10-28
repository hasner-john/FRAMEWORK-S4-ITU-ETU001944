package mg.hasner.framework;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

public class FrontControllerServlet extends HttpServlet {

    private MappingRegistry registry;
    private SpringContext springContext;
    private String viewPrefix;
    private String viewSuffix;
    private String authSessionKey;
    private String rolesSessionKey;

    /**
     * Initialise le servlet et recupere le mapping deja construit par le listener.
     * Si le listener n'a pas encore prepare le registre, le servlet le cree lui-meme.
     */
    @Override
    public void init() throws ServletException {
        super.init();
        viewPrefix = getConfiguredValue("viewPrefix", "/");
        viewSuffix = getConfiguredValue("viewSuffix", ".jsp");
        authSessionKey = getServletContext().getInitParameter("authSessionKey");
        rolesSessionKey = getServletContext().getInitParameter("rolesSessionKey");

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

        Auth auth = route.getMethod().getAnnotation(Auth.class);
        if (auth != null) {
            MaSession session = getAuthenticatedSession(req);
            if (session == null) {
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                        "Authentification requise pour acceder a cette ressource.");
                return;
            }

            if (!hasAuthorizedRole(session, auth.roles())) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Vous ne possedez pas le role requis pour cette ressource.");
                return;
            }
        }

        if (route.getMethod().isAnnotationPresent(APIREST.class)) {
            writeRestResponse(req, res, route);
            return;
        }

        dispatchModelAndView(req, res, route);
    }

    /**
     * Invoque une methode REST et ecrit son resultat JSON directement dans la reponse HTTP.
     */
    private void writeRestResponse(HttpServletRequest req, HttpServletResponse res, RouteMapping route)
            throws ServletException, IOException {
        APIREST apiRest = route.getMethod().getAnnotation(APIREST.class);
        Object result = invokeRoute(req, route);
        String json;

        if (apiRest.alreadyJson() && result instanceof String) {
            json = (String) result;
        } else {
            json = JsonSerializer.toJson(result);
        }

        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write(json);
    }

    /**
     * Invoque la methode du controleur, exige un ModelAndView et fait le forward vers la JSP.
     */
    private void dispatchModelAndView(HttpServletRequest req, HttpServletResponse res, RouteMapping route)
            throws ServletException, IOException {
        Object result = invokeRoute(req, route);

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
    private Object invokeRoute(HttpServletRequest req, RouteMapping route) throws ServletException {
        try {
            Map<String, MaFile> uploadedFiles = UploadManager.readAndStore(req, getServletContext());
            HttpSession httpSession = route.needsSession() ? req.getSession(true) : null;
            MaSession session = httpSession == null ? null
                    : SessionRegistry.getOrCreate(getServletContext(), httpSession);
            Object result = route.invoke(springContext, req.getParameterMap(), uploadedFiles, session);

            if (session != null) {
                session.flushTo(httpSession);
            }
            return result;
        } catch (ReflectiveOperationException e) {
            throw new ServletException("Erreur pendant l'invocation de la methode "
                    + route.getMethod().getName(), e);
        } catch (RuntimeException e) {
            throw new ServletException("Erreur pendant l'invocation de la methode "
                    + route.getMethod().getName(), e);
        } catch (IOException e) {
            throw new ServletException("Erreur pendant la lecture du fichier upload", e);
        }
    }

    /**
     * Verifie la presence de l'attribut d'authentification configure dans la session du client.
     */
    private MaSession getAuthenticatedSession(HttpServletRequest req) {
        if (authSessionKey == null || authSessionKey.trim().isEmpty()) {
            throw new IllegalStateException("Le parametre authSessionKey doit etre configure dans web.xml.");
        }

        HttpSession httpSession = req.getSession(false);
        if (httpSession == null) {
            return null;
        }

        MaSession session = SessionRegistry.getOrCreate(getServletContext(), httpSession);
        return session.getAttribute(authSessionKey) == null ? null : session;
    }

    /**
     * Verifie que la session possede au moins un des roles demandes par l'annotation.
     */
    private boolean hasAuthorizedRole(MaSession session, String[] requiredRoles) {
        if (requiredRoles == null || requiredRoles.length == 0) {
            return true;
        }

        if (rolesSessionKey == null || rolesSessionKey.trim().isEmpty()) {
            throw new IllegalStateException("Le parametre rolesSessionKey doit etre configure dans web.xml.");
        }

        Object storedRoles = session.getAttribute(rolesSessionKey);
        if (storedRoles == null) {
            return false;
        }

        for (String requiredRole : requiredRoles) {
            if (containsRole(storedRoles, requiredRole)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Accepte une collection, un tableau ou une chaine comme stockage des roles de session.
     */
    private boolean containsRole(Object storedRoles, String requiredRole) {
        if (storedRoles instanceof Collection<?>) {
            return ((Collection<?>) storedRoles).contains(requiredRole);
        }

        if (storedRoles.getClass().isArray()) {
            int length = Array.getLength(storedRoles);
            for (int index = 0; index < length; index++) {
                if (requiredRole.equals(String.valueOf(Array.get(storedRoles, index)))) {
                    return true;
                }
            }
            return false;
        }

        return requiredRole.equals(String.valueOf(storedRoles));
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
