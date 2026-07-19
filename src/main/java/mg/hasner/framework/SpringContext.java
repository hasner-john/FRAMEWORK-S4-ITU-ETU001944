package mg.hasner.framework;

import jakarta.servlet.ServletContext;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class SpringContext {
    public static final String SPRING_WEB_CONTEXT_ATTRIBUTE = "org.springframework.web.context.WebApplicationContext.ROOT";
    private static final String DEFAULT_CONFIG_LOCATION = "/WEB-INF/applicationContext.xml";

    private final Object applicationContext;

    /**
     * Encapsule l'ApplicationContext Spring sans obliger le framework a compiler avec les JAR Spring.
     */
    public SpringContext(Object applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Recupere le contexte Spring existant ou le cree depuis applicationContext.xml.
     */
    public static SpringContext fromServletContext(ServletContext servletContext) {
        Object applicationContext = servletContext.getAttribute(SPRING_WEB_CONTEXT_ATTRIBUTE);
        if (applicationContext == null) {
            applicationContext = createApplicationContext(servletContext);
        }
        return new SpringContext(applicationContext);
    }

    /**
     * Cree un ApplicationContext Spring sans dependance directe a Spring a la compilation.
     */
    private static Object createApplicationContext(ServletContext servletContext) {
        String configLocation = servletContext.getInitParameter("contextConfigLocation");
        if (configLocation == null || configLocation.trim().isEmpty()) {
            configLocation = DEFAULT_CONFIG_LOCATION;
        }

        String realPath = servletContext.getRealPath(configLocation.trim());
        if (realPath == null) {
            throw new IllegalStateException("Impossible de localiser le fichier Spring : " + configLocation);
        }

        File configFile = new File(realPath);
        if (!configFile.exists()) {
            throw new IllegalStateException("Fichier Spring introuvable : " + realPath);
        }

        try {
            Class<?> contextClass = Class.forName("org.springframework.context.support.FileSystemXmlApplicationContext");
            Constructor<?> constructor = contextClass.getConstructor(String.class);
            return constructor.newInstance(configFile.getAbsolutePath());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Spring n'est pas disponible. Verifiez spring-context, spring-beans, "
                    + "spring-core et spring-expression dans WEB-INF/lib.", e);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Impossible de demarrer le contexte Spring depuis : "
                    + configFile.getAbsolutePath(), e);
        }
    }

    /**
     * Indique si un vrai contexte Spring a ete trouve au demarrage de l'application.
     */
    public boolean isAvailable() {
        return applicationContext != null;
    }

    /**
     * Retourne un bean Spring par son nom.
     */
    public Object getBean(String beanName) {
        ensureAvailable();
        try {
            Method getBean = applicationContext.getClass().getMethod("getBean", String.class);
            return getBean.invoke(applicationContext, beanName);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Impossible de recuperer le bean Spring : " + beanName, e);
        }
    }

    /**
     * Retourne un bean Spring par son nom puis le caste vers le type attendu.
     */
    public <T> T getBean(String beanName, Class<T> expectedType) {
        return expectedType.cast(getBean(beanName));
    }

    /**
     * Retourne un bean Spring par son type.
     */
    public <T> T getBean(Class<T> expectedType) {
        ensureAvailable();
        try {
            Method getBean = applicationContext.getClass().getMethod("getBean", Class.class);
            return expectedType.cast(getBean.invoke(applicationContext, expectedType));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Impossible de recuperer le bean Spring : " + expectedType.getName(), e);
        }
    }

    /**
     * Evite une erreur silencieuse si l'application n'a pas encore de contexte Spring.
     */
    private void ensureAvailable() {
        if (!isAvailable()) {
            throw new IllegalStateException("Aucun contexte Spring n'a ete trouve. Verifiez les JAR Spring, "
                    + "le context-param contextConfigLocation et applicationContext.xml.");
        }
    }
}
