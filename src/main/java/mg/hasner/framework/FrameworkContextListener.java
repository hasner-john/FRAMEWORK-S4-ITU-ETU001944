package mg.hasner.framework;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class FrameworkContextListener implements ServletContextListener {
    public static final String REGISTRY_ATTRIBUTE = "mg.hasner.framework.mappingRegistry";
    public static final String SPRING_CONTEXT_ATTRIBUTE = "mg.hasner.framework.springContext";

    /**
     * Au demarrage de l'application web, recupere Spring puis scanne les controleurs du framework.
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        SpringContext springContext = SpringContext.fromServletContext(context);
        MappingRegistry registry = new FrameworkInitializer(context).init();

        context.setAttribute(SPRING_CONTEXT_ATTRIBUTE, springContext);
        context.setAttribute(REGISTRY_ATTRIBUTE, registry);
    }

    /**
     * Au shutdown de l'application web, nettoie les objets stockes dans le ServletContext.
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        sce.getServletContext().removeAttribute(REGISTRY_ATTRIBUTE);
        sce.getServletContext().removeAttribute(SPRING_CONTEXT_ATTRIBUTE);
    }
}
