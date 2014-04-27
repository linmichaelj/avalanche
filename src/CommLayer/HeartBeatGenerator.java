package CommLayer;

import CommLayer.Messages.MessageBuilderUtil;
import Util.ConfigProperties;

import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by linmichaelj on 2014-03-31.
 */
public class HeartBeatGenerator extends Thread {
    public CommClient heartBeatCommLink;
    public int heartBeatInterval;
    public Timer timer;

    public HeartBeatGenerator(CommClient heartBeatCommLink){
        this.heartBeatCommLink = heartBeatCommLink;
        heartBeatInterval = ConfigProperties.getIntProperty("HEART_BEAT_INTERVAL");

        timer = new Timer(true);
        timer.scheduleAtFixedRate(new HeartBeatTask(), 0, heartBeatInterval);
    }

    public void stopHeartBeatGenerator(){
        timer.cancel();
    }

    class HeartBeatTask extends TimerTask {
        @Override
        public void run() {
            heartBeatCommLink.sendMessage(MessageBuilderUtil.generateHeartBeatMessage(heartBeatInterval));
        }
    }
}
