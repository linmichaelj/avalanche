package SchedLayer.Centre;

import CommLayer.CommServer;
import CommLayer.Messages.AvalancheMessages;
import CommLayer.Messages.MessageBuilderUtil;
import SchedLayer.UI.MachineAssignment;
import Util.ConfigProperties;

import java.util.Observable;
import java.util.Observer;

/**
 * Created by tomwu on 26/03/14.
 */
public class GlobalSchedulerService implements Observer {

    private CommServer server;
    private GlobalScheduler globalScheduler = null;
    private int gsPort;

    public GlobalSchedulerService(){
        gsPort = ConfigProperties.getIntProperty("GS_PORT");
        server = new CommServer(gsPort, "Server for Global Scheduler");
        server.addInputObserver(this);
        (new Thread(server)).start();
    }

    @Override
    public void update(Observable observable, Object o) {

        AvalancheMessages.Message message;
        if ((message = MessageBuilderUtil.getMessageOfType(o, AvalancheMessages.Message.MessageType.TOPOLOGY_MESSAGE)) != null) {
            System.out.println("[GlobalSchedulerService] Message of type " + message.getMessageType());
            System.out.println("[GlobalSchedulerService] Received new topology to run.");
            globalScheduler = new GlobalScheduler(MachineAssignment.decodeFromMessage(message.getTopologyMessage()));
            globalScheduler.startScheduling();

        } else if((message = MessageBuilderUtil.getMessageOfType(o, AvalancheMessages.Message.MessageType.TASK_STATUS_MESSAGE)) != null){
            System.out.println("[GlobalSchedulerService] Message of type " + message.getMessageType());
            System.out.println("[GlobalSchedulerService] Received task status: " + message.getTaskStatusMessage().getId() + " is done.");
            globalScheduler.processNewTaskStatusMessage(message);

        }
        else{
            System.out.println("[GlobalSchedulerService] Cannot Handle the following request: ");
        }
    }

    public static void main(String [] args){
        new GlobalSchedulerService();
    }
}
