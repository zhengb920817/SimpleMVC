package simplemvc.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by zhengb on 2018-03-05.
 */
@Retention(RUNTIME)
@Target({METHOD })
public @interface ResponseBody {
    String value() default "";
}
