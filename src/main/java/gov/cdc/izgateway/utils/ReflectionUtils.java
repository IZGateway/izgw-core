package gov.cdc.izgateway.utils;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

public class ReflectionUtils {
    private ReflectionUtils() {
    }

    public static <V> V attempt(Callable<V> c) {
        try {
            return c.call();
        } catch (Exception ex) {
            return null;
        }
    }

    public static Field getDeclaredField(Object obj, String field) throws NoSuchFieldException, SecurityException {
        Field f = null;
        Class<?> c = obj.getClass();
        do {
            try {
                f = c.getDeclaredField(field);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
                if (Object.class.equals(c)) {
                    throw e;
                }
            }
        } while (f == null);
        return f;
    }

    public static Object getField(Object obj, String field) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field f = getDeclaredField(obj, field);
        f.setAccessible(true); // NOSONAR: Yes, we know that setAccessible implies reliance on implementation dependent details
        return f.get(obj);
    }

    public static <F> F getField(Object obj, String path, Class<F> fieldClass) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        String[] fields = path.split("\\.");
        for (String field : fields) {
            obj = getField(obj, field);
        }
        return fieldClass.cast(obj);
    }
    
    public static HttpServletResponse unwrapResponse(HttpServletResponse servletResp) {
        return ((servletResp instanceof HttpServletResponseWrapper w) ? 
        		unwrapResponse((HttpServletResponse)w.getResponse()) : 
        		servletResp);
    }

    public static HttpServletRequest unwrapRequest(HttpServletRequest servletReq) {
        return ((servletReq instanceof HttpServletRequestWrapper w)
            ? unwrapRequest(((HttpServletRequest) w.getRequest())) : servletReq);
    }

}
