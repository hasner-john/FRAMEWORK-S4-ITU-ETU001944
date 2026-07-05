package mg.hasner.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UrlMapping {
    String value();

    // Sprint 3 : methode HTTP associee a la route. GET par defaut
    // pour ne pas casser les controleurs ecrits avant le sprint 3.
    HttpMethod methode() default HttpMethod.GET;
}
