package mg.hasner.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UrlMapping {
    /**
     * URL associee a la methode du controleur.
     */
    String value();

    /**
     * Methode HTTP associee a la route. GET est utilise par defaut.
     */
    HttpMethod methode() default HttpMethod.GET;
}
