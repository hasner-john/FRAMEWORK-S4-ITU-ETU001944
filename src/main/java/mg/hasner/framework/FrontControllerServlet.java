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
import java.util.List;

public class FrontControllerServlet extends HttpServlet {

    private List<Class<?>> controllers;
    private List<RouteMapping> routes;

    @Override
    public void init() throws ServletException {
        super.init();
        controllers = scanControllers();
        routes = scanRoutes(controllers);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        processRequest(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        processRequest(req, res);
    }

    private void processRequest(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String url = getRequestedUrl(req);

        res.setContentType("text/html;charset=UTF-8");
        PrintWriter out = res.getWriter();

        out.println("<h2>URL demandee : " + url + "</h2>");

        if ("/".equals(url)) {
            output(out);
            return;
        }

        RouteMapping route = findRoute(url);

        if (route == null) {
            throw new ServletException("URL non reconnue : " + url
                    + ". URLs existantes : " + getExistingUrls());
        }

        out.println("<h3>Mapping trouve</h3>");
        out.println("<p>Controller : <b>" + route.controllerClass.getSimpleName() + "</b></p>");
        out.println("<p>Methode : <b>" + route.method.getName() + "()</b></p>");
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
            for (RouteMapping route : routes) {
                if (route.controllerClass.equals(clazz)) {
                    hasMappedMethod = true;
                    out.println("<li>" + route.url + " -&gt; " + route.method.getName() + "()</li>");
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

    private List<RouteMapping> scanRoutes(List<Class<?>> controllerClasses) {
        List<RouteMapping> result = new ArrayList<>();

        if (controllerClasses == null) {
            return result;
        }

        for (Class<?> clazz : controllerClasses) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(UrlMapping.class)) {
                    UrlMapping annotation = method.getAnnotation(UrlMapping.class);
                    result.add(new RouteMapping(annotation.value(), clazz, method));
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

    private RouteMapping findRoute(String url) {
        for (RouteMapping route : routes) {
            if (route.url.equals(url)) {
                return route;
            }
        }

        return null;
    }

    private String getExistingUrls() {
        if (routes == null || routes.isEmpty()) {
            return "aucune URL disponible";
        }

        List<String> urls = new ArrayList<>();
        for (RouteMapping route : routes) {
            urls.add(route.url);
        }

        return String.join(", ", urls);
    }

    private static class RouteMapping {
        private final String url;
        private final Class<?> controllerClass;
        private final Method method;

        private RouteMapping(String url, Class<?> controllerClass, Method method) {
            this.url = url;
            this.controllerClass = controllerClass;
            this.method = method;
        }
    }
}
