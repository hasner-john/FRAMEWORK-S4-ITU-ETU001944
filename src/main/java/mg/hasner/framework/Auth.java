package mg.hasner.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Protege une methode de controleur et exige un utilisateur authentifie en session.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auth {
    /**
     * Roles autorises. Une liste vide exige seulement une authentification.
     */
    String[] roles() default {};
}
