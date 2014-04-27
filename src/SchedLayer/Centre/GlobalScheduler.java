package SchedLayer.Centre;

import CommLayer.CommClient;
import CommLayer.Messages.AvalancheMessages;
import CommLayer.Messages.MessageBuilderUtil;
import SchedLayer.UI.MachineAssignment;
import Util.ConfigProperties;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

/**
 * Created by tomwu on 26/03/14.
 */
public class GlobalScheduler {
    // Mapping from task id to list of upstream inputs, e.g. {"b1":[], "b2":[], "m1":["b1", "b2"]}
    // Doesn't change after initialization
    private HashMap<String, LinkedList<String>> nodeInputs = new HashMap<String, LinkedList<String>>();
    // Mapping from machine to its the current tasks it is working on
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> machineToTask = new ConcurrentHashMap<String, ConcurrentLinkedQueue<String>>();
    // Mapping from builders to their input paths. Doesn't change after initialization
    private HashMap<String, String> builders = new HashMap<String, String>();
    // Mapping from nodes to its status
    private ConcurrentHashMap<String, GlobalStatus> nodes = new ConcurrentHashMap<String, GlobalStatus>();
    // Task to machine assignment
    private HashMap<String, LinkedList<String>> assignments = new HashMap<String, LinkedList<String>>();

    private int tsPort;

    public GlobalScheduler(MachineAssignment machineAssignment) {
        this.nodeInputs = machineAssignment.getTopology().getNodeInputs();
        System.out.println("NODE INPUTS: " + nodeInputs);
        this.builders = machineAssignment.getTopology().getBuilders();
        this.assignments = machineAssignment.getAssignment();

        HashSet<String> machines = machineAssignment.getMachinePool().getMachines();
        for (String m : machines) {
            this.machineToTask.put(m, new ConcurrentLinkedQueue<String>());
        }

        for (String n : nodeInputs.keySet()) {
            nodes.put(n, GlobalStatus.UNSCHEDULED);
        }

        System.out.println("Global sched construct: machineToTask" + machineToTask);
        System.out.println("Global sched construct: nodes" + nodes);

        tsPort = ConfigProperties.getIntProperty("TS_PORT");
    }

    private void markTaskAsCompleted(String task) {
        nodes.put(task, GlobalStatus.COMPLETED);
        for (String m : machineToTask.keySet()) {
            for (String id : machineToTask.get(m)) {
                if (id.equals(task)) {
                    machineToTask.get(m).remove(id);
                }
            }
        }
    }


    private boolean canBeginTask(String task) {
        for (String input : nodeInputs.get(task)) {
            if (nodes.get(input) != GlobalStatus.COMPLETED) {
                return false;
            }
        }
        return true;

    }

    private String nextTaskToStart() {
        for (String task : nodes.keySet()) {
            // DEBUGGING
            if (nodes.get(task) == GlobalStatus.UNSCHEDULED) {
                return task;
            }
        }
        return "";
    }

    private String nextMachineToUse(String task) {
        return assignments.get(task).getFirst();
    }


    private void beginTaskOnMachine(String task, String machine) {
        machineToTask.get(machine).add(task);
        nodes.put(task, GlobalStatus.SCHEDULED);

        String commId = "Global scheduler client for notifying task init service";
        CommClient tsClient = new CommClient(commId, machine, tsPort);

        AvalancheMessages.Message.Builder initTaskMessage;

        if (builders.containsKey(task)) {
            initTaskMessage = MessageBuilderUtil.generateBuilderInitMessage(task, builders.get(task));
        } else {
            initTaskMessage = MessageBuilderUtil.generateMergerInitMessage(task, nodeInputs.get(task));
        }

        tsClient.sendMessage(initTaskMessage);
        tsClient.stop();
    }


    public void processNewTaskStatusMessage(AvalancheMessages.Message msg) {

        AvalancheMessages.Message.TaskStatusMessage tsm = msg.getTaskStatusMessage();

        String id = tsm.getId();
        AvalancheMessages.Message.TaskStatusMessage.Status status = tsm.getStatus();
        System.out.println("Global Sched: id " + id + " status " + status);
        if (status.equals(AvalancheMessages.Message.TaskStatusMessage.Status.FINISHED)) {
            markTaskAsCompleted(id);
        }

        //System.out.println("Global sched processNewTaskStatusMessage: machineToTask" + machineToTask);
        //System.out.println("Global sched processNewTaskStatusMessage: nodes" + nodes);

        String nextTask = nextTaskToStart();
        System.out.println("Global sched processNewTaskStatusMessage: nextTaskToStart" + nextTask);
        String nextMachine;

        while (!nextTask.isEmpty()) {
            if (!(nextMachine = nextMachineToUse(nextTask)).isEmpty()) {
                beginTaskOnMachine(nextTask, nextMachine);
            }
            nextTask = nextTaskToStart();
        }

    }

    public void startScheduling() {
        String nextTask = nextTaskToStart();
        String nextMachine;

        System.out.println("Started scheduling!!!!");
        while (!nextTask.isEmpty()) {
            System.out.println("Started scheduling!!!! " + nextTask);
            if (!(nextMachine = nextMachineToUse(nextTask)).isEmpty()) {
                beginTaskOnMachine(nextTask, nextMachine);
            }
            nextTask = nextTaskToStart();
        }
        System.out.println("Started scheduling!!!!");
    }


}
