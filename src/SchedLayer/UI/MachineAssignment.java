package SchedLayer.UI;

import SchedLayer.Centre.GlobalStatus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import CommLayer.Messages.AvalancheMessages.Message;

/**
 * Created by tomwu on 05/03/14.
 */
public class MachineAssignment {
    private Topology topology;
    private MachinePool machinePool;
    private String namespace;
    // The eager assignment (from a task to its potential list of machines)
    private HashMap<String, LinkedList<String>> assignment = new HashMap<String, LinkedList<String>>();


    public MachineAssignment(String namespace, String topologyConfigFilename, String machineConfigFilename) {
        this.namespace = namespace;
        this.topology = new Topology(topologyConfigFilename);
        this.machinePool = new MachinePool(machineConfigFilename);
        computeAssignment();
    }

    public MachineAssignment(String namespace, MachinePool machinePool, Topology topology) {
        this.namespace = namespace;
        this.machinePool = machinePool;
        this.topology = topology;
        computeAssignment();
    }

    public Topology getTopology() {
        return this.topology;
    }

    public MachinePool getMachinePool() {
        return this.machinePool;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public HashMap<String, LinkedList<String>> getAssignment() {
        return this.assignment;
    }

    private void assignMachinesToTasks(LinkedList<String> machines, LinkedList<String> tasks) {
        System.out.println("assignMachinesToTasks: " + machines + " AND " + tasks);
        if (machines.size() >= tasks.size()) {
            int machineIndex = 0;
            while (machineIndex < machines.size()) {
                assignment.get(tasks.get(machineIndex % tasks.size())).add(machines.get(machineIndex));
                machineIndex += 1;
            }
        } else {
            int taskIndex = 0;
            while (taskIndex < tasks.size()) {
                assignment.get(tasks.get(taskIndex)).add(machines.get(taskIndex % machines.size()));
                taskIndex += 1;
            }
        }
    }


    private void computeAssignment() {
        for (String k : topology.getNodeInputs().keySet()) {
            assignment.put(k, new LinkedList<String>());
        }

        LinkedList<String> allMachines = new LinkedList<String>();
        for (String m : machinePool.getMachines()) {
            allMachines.add(m);
        }
        System.out.println("computeAssignment: topology " + machinePool.getMachines() + " AND " + assignment);
        LinkedList<String> level = new LinkedList<String>();
        level.add(topology.findRootNode());
        assignMachinesToTasks(allMachines, level);

        System.out.println("EagerGlobalScheduler root node's machines " + assignment.get(topology.findRootNode()));

        while (!level.isEmpty() && !topology.getBuilders().containsKey(level.getFirst())) {
            for (String t : level) {
                assignMachinesToTasks(assignment.get(t), topology.getNodeInputs().get(t));
            }

            level = topology.upstreamTasks(level);
        }
    }


    public Message.Builder encodeToMessage() {
        Message.Builder message = Message.newBuilder();
        message.setMessageType(Message.MessageType.TOPOLOGY_MESSAGE);
        Message.TopologyMessage.Builder innerMessage = Message.TopologyMessage.newBuilder();
        innerMessage.setNamespace(namespace);
        innerMessage.addAllMachines(machinePool.getMachines());

        for (String b : topology.getBuilders().keySet()) {
            Message.TopologyMessage.BuilderNode.Builder builderNode = Message.TopologyMessage.BuilderNode.newBuilder();
            builderNode.setId(b);
            builderNode.setPath(topology.getBuilders().get(b));
            innerMessage.addBuildernodes(builderNode);
        }

        for (String n : topology.getNodeInputs().keySet()) {
            Message.TopologyMessage.Node.Builder node = Message.TopologyMessage.Node.newBuilder();
            node.setName(n);
            node.addAllInputs(topology.getNodeInputs().get(n));
            innerMessage.addNodes(node);
        }

        message.setTopologyMessage(innerMessage);
        return message;

    }


    public static MachineAssignment decodeFromMessage(Message.TopologyMessage message) {
        String namespace = message.getNamespace();
        System.out.println("decode: namespace " + namespace);
        System.out.println("decode: machines " + message.getMachinesList());
        HashSet<String> machines = new HashSet<String>();
        for (String m : message.getMachinesList()) {
            machines.add(m);
        }
        MachinePool machinePool = new MachinePool(machines);

        HashMap<String, String> builders = new HashMap<String, String>();
        for (Message.TopologyMessage.BuilderNode bn : message.getBuildernodesList()) {
            builders.put(bn.getId(), bn.getPath());
        }

        HashMap<String, LinkedList<String>> nodeInputs = new HashMap<String, LinkedList<String>>();
        for (Message.TopologyMessage.Node n : message.getNodesList()) {
            nodeInputs.put(n.getName(), new LinkedList<String>(n.getInputsList()));
        }
        Topology topology = new Topology(nodeInputs, builders);

        return new MachineAssignment(namespace, machinePool, topology);
    }


}


