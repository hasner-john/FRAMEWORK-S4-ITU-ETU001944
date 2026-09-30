package mg.hasner.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indique qu'une methode de controleur produit une reponse REST au format JSON.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface APIREST {

    /**
     * Indique que la valeur String retournee contient deja un document JSON valide.
     * Si false, le framework transforme le resultat en JSON.
     */
    boolean alreadyJson() default false;
}
