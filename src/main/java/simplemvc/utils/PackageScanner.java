package simplemvc.utils;

import java.io.IOException;
import java.util.List;

/**
 * Created by zhengb on 2018-03-05.
 */
public interface PackageScanner {
    List<String> getFullyQualifiedClassNameList() throws IOException;
}
