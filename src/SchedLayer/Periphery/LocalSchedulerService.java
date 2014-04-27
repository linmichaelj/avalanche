package SchedLayer.Periphery;

import CommLayer.CommServer;
import CommLayer.Messages.AvalancheMessages.Message;
import CommLayer.Messages.MessageBuilderUtil;
import CommLayer.StreamThread;
import Util.ConfigProperties;

import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;


public class LocalSchedulerService implements Observer {
    CommServer server;
    private LocalScheduler localScheduler;
    private int lsPort;
    private ConcurrentHashMap<String, StreamThread> taskStreamThreads;

    public LocalSchedulerService(){
        localScheduler = new LocalScheduler();
        lsPort = ConfigProperties.getIntProperty("LS_PORT");
        taskStreamThreads = new ConcurrentHashMap<String, StreamThread>();
        server = new CommServer(lsPort, "Server for Local Scheduler");
        server.addInputObserver(this);
        (new Thread(server)).start();
    }

    @Override
    public void update(Observable observable, Object o) {
        Message message;
        if((message = MessageBuilderUtil.getMessageOfType(o, Message.MessageType.TASK_STATUS_MESSAGE)) != null){
            StreamThread serverThread = (StreamThread) observable;

            System.out.println("[LocalSchedulerService] Received initialization Request.");
            Message.TaskStatusMessage innerMessage = message.getTaskStatusMessage();

            System.out.println("[LocalSchedulerService] Task " + innerMessage.getId() + " has changed status to " + innerMessage.getStatus());
            taskStreamThreads.put(innerMessage.getId(), serverThread);
            localScheduler.setTaskStatus(innerMessage.getId(), innerMessage.getStatus(), message);

            if (localScheduler.canRunNewTask()){
                String idToRun = localScheduler.nextTaskToRun();
                System.out.println("[LocalSchedulerService] Will let the task " + idToRun + "run next");
                taskStreamThreads.get(idToRun).sendMessage(MessageBuilderUtil.generateTaskSetStateMessage(idToRun, Message.TaskSetStateMessage.State.RUN));
            }
        }
        else{
            System.out.println("[LocalSchedulerService] Cannot Handle the following request: ");
        }
    }

    public static void main(String [] args){
        new LocalSchedulerService();
    }


}

