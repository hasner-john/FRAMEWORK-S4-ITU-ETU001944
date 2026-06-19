import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FrontControllerServlet extends HttpServlet {

    // Liste des classes controleurs trouvees au demarrage (cache)
    private List<Class<?>> controllers;

    @Override
    public void init() throws ServletException {
        super.init();
        controllers = scanControllers();
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        processRequest(req, res);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        processRequest(req, res);
    }

    // Sprint0 : traite la requete, sprint1 : affiche aussi les controleurs trouves
    private void processRequest(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String url = req.getRequestURI().toString();

        res.setContentType("text/html;charset=UTF-8");
        PrintWriter out = res.getWriter();

        out.println("<h2>URL demandee : " + url + "</h2>");

        output(out);
    }

    // Affiche dans le navigateur la liste des @Controller trouves et leurs @GetMapping
    private void output(PrintWriter out) {
        out.println("<h3>Controleurs detectes</h3>");

        if (controllers == null || controllers.isEmpty()) {
            out.println("<p>Aucun controleur trouve.</p>");
            return;
        }

        out.println("<ul>");
        for (Class<?> clazz : controllers) {
            Controller controllerAnnotation = clazz.getAnnotation(Controller.class);
            out.println("<li><b>" + clazz.getSimpleName() + "</b>");

            out.println("<ul>");
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.isAnnotationPresent(GetMapping.class)) {
                    GetMapping mapping = m.getAnnotation(GetMapping.class);
                    out.println("<li>" + mapping.value() + " -&gt; " + m.getName() + "()</li>");
                }
            }
            out.println("</ul>");

            out.println("</li>");
        }
        out.println("</ul>");
    }

    // Parcourt WEB-INF/classes (racine du classloader, pas de packages) et
    // garde uniquement les classes annotees @Controller
    private List<Class<?>> scanControllers() {
        List<Class<?>> result = new ArrayList<>();

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL classesUrl = classLoader.getResource(".");

            if (classesUrl == null) {
                // fallback : on tente de localiser via une classe connue
                classesUrl = FrontControllerServlet.class.getResource("/");
            }

            if (classesUrl == null) {
                return result;
            }

            File classesDir = new File(classesUrl.toURI());

            File[] files = classesDir.listFiles((dir, name) -> name.endsWith(".class"));
            if (files == null) {
                return result;
            }

            for (File f : files) {
                String className = f.getName().replace(".class", "");
                try {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(Controller.class)) {
                        result.add(clazz);
                    }
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    // on ignore les classes non chargeables (ex: annotations elles-memes)
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
}
