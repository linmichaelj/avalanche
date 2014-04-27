package Core.Test;

import java.util.TimerTask;

/**
 * Created by linmichaelj on 2014-03-31.
 */
public class ComponentFailureTask extends TimerTask {

    @Override
    public void run() {
        System.out.println("DETECTED FAILED COMPONENT");
    }

}
