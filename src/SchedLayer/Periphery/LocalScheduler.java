package SchedLayer.Periphery;
import CommLayer.CommClient;
import CommLayer.Messages.AvalancheMessages.Message;
import Util.ConfigProperties;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


public class LocalScheduler {

    private static final int MAX_CONCURRENT = 5;

    private ConcurrentHashMap<String, Message.TaskStatusMessage.Status> tasks; // Tasks that have not finished
    private ConcurrentLinkedQueue<String> finished_tasks;
    private ConcurrentLinkedQueue<String> running_tasks;
    private ConcurrentLinkedQueue<String> ready_tasks;

    private String gsHost;
    private int gsPort;
    private String msHost;
    private int msPort;


    public LocalScheduler() {
        tasks = new ConcurrentHashMap<String, Message.TaskStatusMessage.Status>();
        finished_tasks = new ConcurrentLinkedQueue<String>();
        gsHost = ConfigProperties.getProperty("GS_HOST");
        gsPort = ConfigProperties.getIntProperty("GS_PORT");
        msHost = ConfigProperties.getProperty("MS_HOST");
        msPort = ConfigProperties.getIntProperty("MS_PORT");
    }

    public void setTaskStatus(String id, Message.TaskStatusMessage.Status status, Message message) {
        tasks.put(id, status);

        if (status == Message.TaskStatusMessage.Status.FINISHED) {
            tasks.remove(id);
            finished_tasks.add(id);
            System.out.println("LocalSched: going to notify global scheduler that task " +
                    message.getTaskStatusMessage().getId() + " is done");
            notifyGlobalScheduler(message);
            notifyMessageService(message);
        }

        running_tasks = getTasksByStatus(Message.TaskStatusMessage.Status.RUNNING);
        ready_tasks = getTasksByStatus(Message.TaskStatusMessage.Status.READY);
    }

    private ConcurrentLinkedQueue<String> getTasksByStatus(Message.TaskStatusMessage.Status status) {
        ConcurrentLinkedQueue<String> tasksByStatus = new ConcurrentLinkedQueue<String>();

        for (String id : tasks.keySet()) {
            if (tasks.get(id).equals(status)) {
                tasksByStatus.add(id);
            }
        }

        return tasksByStatus;
    }

    public boolean canRunNewTask() {
        return running_tasks.size() < MAX_CONCURRENT && ready_tasks.size() > 0;
    }

    public String nextTaskToRun() {
        return ready_tasks.iterator().next();
    }

    private void notifyGlobalScheduler(Message message) {
        String commId = "Local scheduler client for notifying global scheduler";
        CommClient gsClient = new CommClient(commId, gsHost, gsPort);
        Message.Builder messageBuilder = Message.newBuilder();
        messageBuilder.mergeFrom(message);
        gsClient.sendMessage(messageBuilder);
        gsClient.stop();
    }

    private void notifyMessageService(Message message) {
        String commId = "Local scheduler client for notifying message service";
        CommClient msClient = new CommClient(commId, msHost, msPort);
        Message.Builder messageBuilder = Message.newBuilder();
        messageBuilder.mergeFrom(message);
        msClient.sendMessage(messageBuilder);
        msClient.stop();
    }
}
