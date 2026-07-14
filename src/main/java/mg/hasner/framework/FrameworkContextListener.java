package mg.hasner.framework;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class FrameworkContextListener implements ServletContextListener {
    public static final String REGISTRY_ATTRIBUTE = "mg.hasner.framework.mappingRegistry";

    /**
     * Au demarrage de l'application web, scanne les controleurs et stocke le registre dans le ServletContext.
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        MappingRegistry registry = new FrameworkInitializer(context).init();
        context.setAttribute(REGISTRY_ATTRIBUTE, registry);
    }

    /**
     * Au shutdown de l'application web, nettoie le registre stocke dans le ServletContext.
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        sce.getServletContext().removeAttribute(REGISTRY_ATTRIBUTE);
    }
}
