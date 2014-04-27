package CommLayer;

import CommLayer.Messages.AvalancheMessages.Message;
import CommLayer.Messages.MessageBuilderUtil;
import Util.ConfigProperties;

import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by linmichaelj on 2014-03-31.
 */
public class HeartBeatMonitor implements Observer {
    private Class<?> timerTaskClass;
    private int acceptableHeartBeatInterval;
    ConcurrentHashMap<String, Timer> timerConcurrentHashMap;

    public HeartBeatMonitor(Class <?> timerTaskClass){
        this.timerTaskClass = timerTaskClass;
        acceptableHeartBeatInterval = ConfigProperties.getIntProperty("HEART_BEAT_INTERVAL") + ConfigProperties.getIntProperty("HEART_BEAT_THRESHOLD");
        timerConcurrentHashMap = new ConcurrentHashMap<String, Timer>();
    }

    @Override
    public void update(Observable o, Object arg) {
        Message message;
        if((message = MessageBuilderUtil.getMessageOfType(arg, Message.MessageType.HEART_BEAT_MESSAGE)) != null){
            String id = message.getSenderId();
            Timer timer;
            if((timer = timerConcurrentHashMap.get(id)) != null){
                timer.cancel();
            }
            Timer tempTimer = new Timer(true);

            try {
                tempTimer.schedule((java.util.TimerTask) timerTaskClass.newInstance(), acceptableHeartBeatInterval);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            timerConcurrentHashMap.put(id, tempTimer);
        } else if((message = MessageBuilderUtil.getMessageOfType(arg, Message.MessageType.BYE)) != null){
            Timer timer;
            if((timer = timerConcurrentHashMap.get(message.getSenderId())) != null){
                timer.cancel();
            }
        }
    }
}
