package Core.Test;

import CommLayer.CommServer;
import CommLayer.HeartBeatMonitor;
import CommLayer.Messages.AvalancheMessages.Message;
import CommLayer.Messages.MessageBuilderUtil;
import CommLayer.StreamThread;
import Util.ConfigProperties;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by linmichaelj on 3/5/2014.
 */
public class LocalSchedulerDummy implements Observer {
    BufferedReader bufferRead;
    CommServer server;
    ConcurrentHashMap<String, HeartBeatMonitor> heartBeatMonitorHashMap;

    public LocalSchedulerDummy(){
        bufferRead = new BufferedReader(new InputStreamReader(System.in));
        heartBeatMonitorHashMap = new ConcurrentHashMap<String, HeartBeatMonitor>();
        server = new CommServer(ConfigProperties.getIntProperty("LS_PORT"), "Server for Local Scheduler");
        server.addInputObserver(this);
        server.addInputObserver(new HeartBeatMonitor(ComponentFailureTask.class));

        (new Thread(server)).start();
    }

    @Override
    public void update(Observable observable, Object o) {
        Message message;
        if((message = MessageBuilderUtil.getMessageOfType(o, Message.MessageType.TASK_STATUS_MESSAGE)) != null){
            Message.TaskStatusMessage innerMessage = message.getTaskStatusMessage();
            System.out.println("Task " + innerMessage + " has changed status to " + innerMessage.getStatus());

            if(innerMessage.getStatus() == Message.TaskStatusMessage.Status.READY){
                System.out.println("Press Enter To Allow Task to Run");
                try {
                    bufferRead.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                StreamThread serverThread = (StreamThread) observable;
                serverThread.sendMessage(MessageBuilderUtil.generateTaskSetStateMessage(message.getTaskStatusMessage().getId(), Message.TaskSetStateMessage.State.RUN));
            }
        } else if (MessageBuilderUtil.getMessageOfType(o, Message.MessageType.HEART_BEAT_MESSAGE) == null ){
            System.out.println("[Local Sched] Cannot Handle the following request: ");
        }
    }

    public static void main(String [] args){
        new LocalSchedulerDummy();
    }
}
