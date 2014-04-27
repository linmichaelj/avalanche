package Util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by linmichaelj on 2014-03-26.
 */
public class ConfigProperties {
    public static int getIntProperty(String field){
        return Integer.parseInt(getProperty(field));
    }

    public static String getProperty(String field){
        Properties prop = new Properties();
        try {
            InputStream input = new FileInputStream("config.properties");
            prop.load(input);
            return prop.getProperty(field);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
