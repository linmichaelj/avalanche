package SchedLayer.Message;

import CommLayer.CommServer;
import CommLayer.Messages.AvalancheMessages.Message;
import CommLayer.Messages.MessageBuilderUtil;
import CommLayer.StreamThread;
import Util.ConfigProperties;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by tomwu on 29/03/14.
 */
public class MessageService implements Observer {

    private CommServer server;
    private int msPort;
    private ConcurrentHashMap<String, Message.TaskStatusMessage> statuses;


    public MessageService(){
        msPort = ConfigProperties.getIntProperty("MS_PORT");
        statuses = new ConcurrentHashMap<String, Message.TaskStatusMessage>();
        server = new CommServer(msPort, "Server for Message Service");
        server.addInputObserver(this);
        (new Thread(server)).start();
    }

    @Override
    public void update(Observable observable, Object o) {
        Message message;
        if ((message = MessageBuilderUtil.getMessageOfType(o, Message.MessageType.TOPOLOGY_STATUS_MESSAGE)) != null) {
            // Retreving message
            StreamThread serverThread = (StreamThread) observable;
            serverThread.sendMessage(MessageBuilderUtil.generateTopologyStatusMessage(
                    new LinkedList<Message.TaskStatusMessage>(statuses.values())
            ));

        } else if((message = MessageBuilderUtil.getMessageOfType(o, Message.MessageType.TASK_STATUS_MESSAGE)) != null){
            // Writing message
            System.out.println("[MessageService] Message of type " + message.getMessageType());
            System.out.println("[MessageService] Received task status: " + message.getTaskStatusMessage().getId());
            String id = message.getTaskStatusMessage().getId();
            statuses.put(id, message.getTaskStatusMessage());

        } else{
            System.out.println("[MessageService] Cannot Handle the following request: ");

        }

    }

    public static void main(String [] args){
        new MessageService();
    }

}

