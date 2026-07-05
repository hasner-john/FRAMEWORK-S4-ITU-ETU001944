package mg.hasner.framework;

import java.lang.reflect.Method;

public class RouteMapping {
    private final UrlMethode urlMethode;
    private final Class<?> controllerClass;
    private final Method method;

    public RouteMapping(UrlMethode urlMethode, Class<?> controllerClass, Method method) {
        this.urlMethode = urlMethode;
        this.controllerClass = controllerClass;
        this.method = method;
    }

    public UrlMethode getUrlMethode() {
        return urlMethode;
    }

    public String getUrl() {
        return urlMethode.getUrl();
    }

    public HttpMethod getHttpMethod() {
        return urlMethode.getMethode();
    }

    public Class<?> getControllerClass() {
        return controllerClass;
    }

    public Method getMethod() {
        return method;
    }

    public Object invoke() throws ReflectiveOperationException {
        Object controller = controllerClass.getDeclaredConstructor().newInstance();
        method.setAccessible(true);
        return method.invoke(controller);
    }
}
