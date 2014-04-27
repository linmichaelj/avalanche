package DataLayer.Utils;

import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;

import java.io.File;
import java.io.IOException;

/**
 * Created by linmichaelj on 1/12/2014.
 */
public class DbHelper {
    public static DB getConnection() throws IOException {
        Options options = new Options();
        options.createIfMissing(true);
        JniDBFactory factory = new JniDBFactory();
        return factory.open(new File("TaskOutputDB"), options);

    }
}
