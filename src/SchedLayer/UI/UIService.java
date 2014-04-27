package SchedLayer.UI;

import CommLayer.CommClient;
import CommLayer.Messages.AvalancheMessages;
import CommLayer.Messages.MessageBuilderUtil;
import CommLayer.StreamThread;
import SchedLayer.Centre.GlobalStatus;
import Util.ConfigProperties;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by tomwu on 05/03/14.
 */
public class UIService implements Observer{
    private String uiTopologyConfig;
    private String uiMachineConfig;
    private String namespace;
    private String gsHost;
    private Integer gsPort;
    private Integer uiServiceSleepTime;
    private String msHost;
    private int msPort;
    private String task_status_file;

    private CommClient msClient;
    private MachineAssignment machineAssignment;
    private ConcurrentHashMap<String, AvalancheMessages.Message.TaskStatusMessage> statuses = new ConcurrentHashMap<String, AvalancheMessages.Message.TaskStatusMessage>();
    private Boolean started = false;

    public UIService() {
        loadConfigProperties();
        System.out.println("uiTopologyConfig: " + uiTopologyConfig);

        File topologyFile, machineFile;
        topologyFile = new File(uiTopologyConfig);
        machineFile = new File(uiMachineConfig);

        msClient = new CommClient("Client for the Message Server",msHost, msPort);
        System.out.println("Started client for Message Server");
        msClient.addInputObserver(this);
        AvalancheMessages.Message message = MessageBuilderUtil.generateTopologyStatusMessage(
            new LinkedList<AvalancheMessages.Message.TaskStatusMessage>()
        ).build();

        while (true) {
            // Start a new topology to the global scheduler only when the new files exist
            if(!topologyFile.exists() || !machineFile.exists()) {
                System.out.println("FILE DOES NOT EXIST");
                try {
                    Thread.sleep(uiServiceSleepTime);
                    // Request status update
                    if (started) {
                        AvalancheMessages.Message.Builder messageBuilder = AvalancheMessages.Message.newBuilder();
                        messageBuilder.mergeFrom(message);
                        msClient.sendMessage(messageBuilder);
                        saveTaskStatusToFile(task_status_file);
                    }
                } catch (Exception e) {
                    System.err.println("[UIService] Config file does not exist. Sleep got interrupted.");
                }
                continue;
            }

            System.out.println("New topology started");

            started = true;
            machineAssignment = new MachineAssignment(namespace, uiTopologyConfig, uiMachineConfig);

            // Tell global scheduler to start the request.
            String commId = "Topology client for namespace  " + namespace;
            CommClient topologyClient = new CommClient(commId, gsHost, gsPort);
            topologyClient.sendMessage(machineAssignment.encodeToMessage());

            // Delete the topology files after the request is started.
            topologyFile.delete();
            machineFile.delete();
            System.out.println("File deleted");
        }

    }

    public static void main(String[] args) {
        new UIService();
    }

    @Override
    public void update(Observable observable, Object o) {
        // Print the received message type
        AvalancheMessages.Message message = (AvalancheMessages.Message) o;
        System.out.println("[UIService] Received the following message type from message server: " + message.getMessageType());

        if ((message = MessageBuilderUtil.getMessageOfType(o, AvalancheMessages.Message.MessageType.TOPOLOGY_STATUS_MESSAGE)) != null) {
            System.out.println("[UIService] Received topology status: " + message.getTopologyStatusMessage().getTaskStatusesList());
            for (AvalancheMessages.Message.TaskStatusMessage taskStatusMessage :  message.getTopologyStatusMessage().getTaskStatusesList()) {
                statuses.put(taskStatusMessage.getId(), taskStatusMessage);
            }

        }  else{
            System.out.println("[UIService] Cannot Handle the following request: ");
        }

    }

    private void loadConfigProperties() {
        uiTopologyConfig = ConfigProperties.getProperty("UI_TOPOLOGY_CONFIG");
        uiMachineConfig = ConfigProperties.getProperty("UI_MACHINE_CONFIG");
        namespace = ConfigProperties.getProperty("NAMESPACE");
        gsHost = ConfigProperties.getProperty("GS_HOST");
        gsPort = ConfigProperties.getIntProperty("GS_PORT");
        uiServiceSleepTime = ConfigProperties.getIntProperty("UI_SERVICE_SLEEP_TIME");
        msHost = ConfigProperties.getProperty("MS_HOST");
        msPort = ConfigProperties.getIntProperty("MS_PORT");
        task_status_file = ConfigProperties.getProperty("TASK_STATUS_FILE");
    }

    private void saveTaskStatusToFile(String filename) {
        try {
            PrintWriter writer = new PrintWriter(filename);
            writer.println("// Task_Status_File");
            System.out.println("Writing to file!!!!");
            for (String n : statuses.keySet()) {
                AvalancheMessages.Message.TaskStatusMessage taskStatus = statuses.get(n);
                String percent;
                if (taskStatus.getStatus() == AvalancheMessages.Message.TaskStatusMessage.Status.FINISHED) {
                    percent = "100";
                } else if (taskStatus.getStatus() == AvalancheMessages.Message.TaskStatusMessage.Status.RUNNING) {
                    percent = "50";
                } else {
                    percent = "0";
                }
                writer.println(n + " " + percent);
            }
            writer.close();
        } catch (Exception e) {
            System.err.println("Could not save task status to file.");
        }
    }


}
