package mg.hasner.framework;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class FrameworkContextListener implements ServletContextListener {
    public static final String REGISTRY_ATTRIBUTE = "mg.hasner.framework.mappingRegistry";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        MappingRegistry registry = new FrameworkInitializer(context).init();
        context.setAttribute(REGISTRY_ATTRIBUTE, registry);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        sce.getServletContext().removeAttribute(REGISTRY_ATTRIBUTE);
    }
}
