package Core.Test;

import CommLayer.CommServer;
import CommLayer.Messages.AvalancheMessages.Message;
import CommLayer.Messages.MessageBuilderUtil;
import CommLayer.StreamThread;
import Util.ConfigProperties;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by linmichaelj on 3/5/2014.
 */
public class PauseTestScheduler implements Observer {
    BufferedReader bufferRead;
    CommServer server;
    boolean pause;

    public PauseTestScheduler(){
        bufferRead = new BufferedReader(new InputStreamReader(System.in));
        pause = true;
        server = new CommServer(ConfigProperties.getIntProperty("LS_PORT"), "Server for Local Scheduler");
        server.addInputObserver(this);
        (new Thread(server)).start();
    }

    @Override
    public void update(Observable observable, Object o) {
        Message message;
        if((message = MessageBuilderUtil.getMessageOfType(o, Message.MessageType.TASK_STATUS_MESSAGE)) != null){
            Message.TaskStatusMessage innerMessage = message.getTaskStatusMessage();
            if(innerMessage.getStatus() == Message.TaskStatusMessage.Status.READY){
                StreamThread serverThread = (StreamThread) observable;
                serverThread.sendMessage(MessageBuilderUtil.generateTaskSetStateMessage(message.getTaskStatusMessage().getId(), Message.TaskSetStateMessage.State.RUN));
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else if (innerMessage.getStatus() == Message.TaskStatusMessage.Status.RUNNING && pause){
                StreamThread serverThread = (StreamThread) observable;
                serverThread.sendMessage(MessageBuilderUtil.generateTaskSetStateMessage(message.getTaskStatusMessage().getId(), Message.TaskSetStateMessage.State.PAUSE));

                System.out.println("Press enter to unpause task");
                try {
                    bufferRead.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                serverThread.sendMessage(MessageBuilderUtil.generateTaskSetStateMessage(message.getTaskStatusMessage().getId(), Message.TaskSetStateMessage.State.RESUME));
                pause = false;
            }
        }
        else{
            //System.out.println("[Local Sched] Cannot Handle the following request: ");
        }
    }

    public static void main(String [] args){
        new PauseTestScheduler();
    }
}
