package mg.hasner.framework;

import jakarta.servlet.ServletContext;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FrameworkInitializer {
    private final ServletContext servletContext;

    /**
     * Recoit le ServletContext pour retrouver les classes de l'application web.
     */
    public FrameworkInitializer(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    /**
     * Lance l'initialisation du framework : scan des controleurs puis creation des routes.
     */
    public MappingRegistry init() {
        List<Class<?>> controllers = scanControllers();
        Map<UrlMethode, RouteMapping> routes = scanRoutes(controllers);
        return new MappingRegistry(controllers, routes);
    }

    /**
     * Parcourt WEB-INF/classes et garde les classes annotees avec @Controller.
     */
    private List<Class<?>> scanControllers() {
        List<Class<?>> result = new ArrayList<>();
        File classesDir = getClassesDirectory();

        if (classesDir == null || !classesDir.exists()) {
            return result;
        }

        scanDirectory(classesDir, classesDir, result);
        return result;
    }

    /**
     * Retourne le dossier physique WEB-INF/classes de l'application web.
     */
    private File getClassesDirectory() {
        String realPath = servletContext.getRealPath("/WEB-INF/classes");

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

    /**
     * Scanne recursivement les fichiers .class pour supporter les packages Java.
     */
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

    /**
     * Charge une classe et l'ajoute si elle est annotee @Controller.
     */
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

    /**
     * Convertit un chemin de fichier .class en nom complet de classe Java.
     */
    private String toClassName(File rootDir, File classFile) {
        String rootPath = rootDir.getAbsolutePath();
        String classPath = classFile.getAbsolutePath();
        String relativePath = classPath.substring(rootPath.length() + 1);

        return relativePath
                .replace(File.separatorChar, '.')
                .replace(".class", "");
    }

    /**
     * Construit la table de routes et detecte les conflits URL + methode HTTP.
     */
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
                        RouteMapping existing = result.get(key);
                        throw new RuntimeException("Conflit de routing : " + key
                                + " est deja mappe sur "
                                + existing.getControllerClass().getSimpleName() + "." + existing.getMethod().getName()
                                + "(), impossible de le remapper sur "
                                + clazz.getSimpleName() + "." + method.getName() + "()");
                    }

                    result.put(key, new RouteMapping(key, clazz, method));
                }
            }
        }

        return result;
    }
}
