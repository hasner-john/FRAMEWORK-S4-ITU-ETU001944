package mg.hasner.framework;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FrontControllerServlet extends HttpServlet {

    private List<Class<?>> controllers;

    // Sprint 3 : la cle n'est plus juste l'URL (String) mais le couple
    // (URL + methode HTTP) via UrlMethode. Ca permet d'avoir un meme
    // chemin "/foo" mappe a 2 methodes differentes : une en GET, une en POST.
    private Map<UrlMethode, RouteMapping> routes;

    @Override
    public void init() throws ServletException {
        super.init();
        controllers = scanControllers();
        routes = scanRoutes(controllers);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        // On precise explicitement la methode HTTP de la requete entrante
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

        // La recherche de route se fait maintenant sur le couple url+methode
        UrlMethode key = new UrlMethode(url, httpMethod);
        RouteMapping route = routes.get(key);

        if (route == null) {
            throw new ServletException("URL/Methode non reconnue : " + httpMethod + " " + url
                    + ". URLs existantes : " + getExistingUrls());
        }

        out.println("<h3>Mapping trouve</h3>");
        out.println("<p>Controller : <b>" + route.controllerClass.getSimpleName() + "</b></p>");
        out.println("<p>Methode : <b>" + route.method.getName() + "()</b></p>");
        out.println("<p>HTTP : <b>" + httpMethod + "</b></p>");
    }

    private void output(PrintWriter out) {
        out.println("<h3>Controleurs detectes</h3>");

        if (controllers == null || controllers.isEmpty()) {
            out.println("<p>Aucun controleur trouve.</p>");
            return;
        }

        out.println("<ul>");
        for (Class<?> clazz : controllers) {
            out.println("<li><b>" + clazz.getSimpleName() + "</b>");

            out.println("<ul>");
            boolean hasMappedMethod = false;
            for (RouteMapping route : routes.values()) {
                if (route.controllerClass.equals(clazz)) {
                    hasMappedMethod = true;
                    out.println("<li>[" + route.httpMethod + "] " + route.url
                            + " -&gt; " + route.method.getName() + "()</li>");
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

    private List<Class<?>> scanControllers() {
        List<Class<?>> result = new ArrayList<>();
        File classesDir = getClassesDirectory();

        if (classesDir == null || !classesDir.exists()) {
            return result;
        }

        scanDirectory(classesDir, classesDir, result);
        return result;
    }

    private File getClassesDirectory() {
        String realPath = getServletContext().getRealPath("/WEB-INF/classes");

        if (realPath != null) {
            return new File(realPath);
        }

        try {
            URL classesUrl = Thread.currentThread().getContextClassLoader().getResource("");
            if (classesUrl != null) {
                return new File(classesUrl.toURI());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void scanDirectory(File rootDir, File currentDir, List<Class<?>> result) {
        File[] files = currentDir.listFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(rootDir, file, result);
            } else if (file.getName().endsWith(".class") && !file.getName().contains("$")) {
                addControllerClass(rootDir, file, result);
            }
        }
    }

    private void addControllerClass(File rootDir, File classFile, List<Class<?>> result) {
        String className = toClassName(rootDir, classFile);

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Class<?> clazz = Class.forName(className, false, classLoader);
            if (clazz.isAnnotationPresent(Controller.class)) {
                result.add(clazz);
            }
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            // Ignore les classes non chargeables.
        }
    }

    private String toClassName(File rootDir, File classFile) {
        String rootPath = rootDir.getAbsolutePath();
        String classPath = classFile.getAbsolutePath();
        String relativePath = classPath.substring(rootPath.length() + 1);

        return relativePath
                .replace(File.separatorChar, '.')
                .replace(".class", "");
    }

    // Sprint 3 : construit la Map<UrlMethode, RouteMapping> et leve une
    // exception si deux methodes partagent exactement le meme couple
    // (url, methode HTTP) -> grace a equals/hashCode de UrlMethode,
    // routes.containsKey(key) detecte le doublon avant de l'ecraser.
    private Map<UrlMethode, RouteMapping> scanRoutes(List<Class<?>> controllerClasses) {
        Map<UrlMethode, RouteMapping> result = new HashMap<>();

        if (controllerClasses == null) {
            return result;
        }

        for (Class<?> clazz : controllerClasses) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(UrlMapping.class)) {
                    UrlMapping annotation = method.getAnnotation(UrlMapping.class);
                    UrlMethode key = new UrlMethode(annotation.value(), annotation.methode());

                    if (result.containsKey(key)) {
                        // Doublon detecte : meme url + meme methode HTTP deja mappes
                        RouteMapping existing = result.get(key);
                        throw new RuntimeException("Conflit de routing : " + key
                                + " est deja mappe sur "
                                + existing.controllerClass.getSimpleName() + "." + existing.method.getName()
                                + "(), impossible de le remapper sur "
                                + clazz.getSimpleName() + "." + method.getName() + "()");
                    }

                    result.put(key, new RouteMapping(annotation.value(), annotation.methode(), clazz, method));
                }
            }
        }

        return result;
    }

    private String getRequestedUrl(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();

        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }

        return uri.isEmpty() ? "/" : uri;
    }

    private String getExistingUrls() {
        if (routes == null || routes.isEmpty()) {
            return "aucune URL disponible";
        }

        List<String> urls = new ArrayList<>();
        for (UrlMethode key : routes.keySet()) {
            urls.add(key.toString());
        }

        return String.join(", ", urls);
    }

    // RouteMapping garde maintenant aussi la methode HTTP (httpMethod)
    private static class RouteMapping {
        private final String url;
        private final HttpMethod httpMethod;
        private final Class<?> controllerClass;
        private final Method method;

        private RouteMapping(String url, HttpMethod httpMethod, Class<?> controllerClass, Method method) {
            this.url = url;
            this.httpMethod = httpMethod;
            this.controllerClass = controllerClass;
            this.method = method;
        }
    }
}
