package simplemvc.utils;

import java.util.List;

/**
 * Created by zhengb on 2018-03-05.
 */
public class CollectionUtils {

    public static boolean isEmptyList(List<?> list){
        return  (list == null || list.isEmpty());
    }
}
