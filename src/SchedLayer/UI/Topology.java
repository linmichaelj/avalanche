package SchedLayer.UI;

import SchedLayer.Centre.GlobalStatus;
import Util.ConfigProperties;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * Created by tomwu on 05/03/14.
 */
public class Topology {
    private String rawData;
    private String configFilename;

    // Mapping from task id to list of upstream inputs, e.g. {"b1":[], "b2":[], "m1":["b1", "b2"]}
    // Doesn't change after initialization
    private HashMap<String, LinkedList<String>> nodeInputs = new HashMap<String, LinkedList<String>>();
    // Mapping from builders to their input paths. Doesn't change after initialization
    private HashMap<String, String> builders = new HashMap<String, String>();

    public Topology(String configFilename) {
        this.rawData = ConfigProperties.getProperty("BUILDER_INPUT");
        this.configFilename = configFilename;
        this.loadTopologyFromFile();
    }

    public Topology(HashMap<String, LinkedList<String>> nodeInputs, HashMap<String, String> builders) {
        this.nodeInputs = nodeInputs;
        this.builders = builders;
    }

    private void loadTopologyFromFile() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(this.configFilename));
            try {
                String line = br.readLine();
                while (line != null) {
                    String[] args = line.split(" ");
                    String task = args[0];

                    this.nodeInputs.put(task, new LinkedList<String>());

                    if (args.length == 1) {
                        this.builders.put(task, rawData);
                    }

                    for (int i = 1 ; i < args.length ; i++) {
                        this.nodeInputs.get(task).add(args[i]);
                    }

                    line = br.readLine();
                }

            } finally {
                br.close();
            }

        } catch (Exception e) {
            System.out.println("NewSchedulingLayer.UI.Topology: could not load the topology from config file");
        }
    }

    public HashMap<String, LinkedList<String>> getNodeInputs() {
        return this.nodeInputs;
    }

    public HashMap<String, String> getBuilders() {
        return this.builders;
    }

    public String findRootNode() {
        HashMap<String, Boolean> isRootNode = new HashMap<String, Boolean>();
        for (String n : nodeInputs.keySet()) {
            isRootNode.put(n, true);
        }

        for (String dest : nodeInputs.keySet()) {
            for (String src : nodeInputs.get(dest)) {
                isRootNode.put(src, false);
            }
        }

        for (String n : isRootNode.keySet()) {
            if (isRootNode.get(n)) {
                return n;
            }
        }

        return "";
    }


    /*
     * Given a list of tasks (on the same level), return a list of its upstream tasks.
     */
    public LinkedList<String> upstreamTasks(LinkedList<String> tasks) {
        LinkedList<String> level = new LinkedList<String>();
        for (String t : tasks) {
            for (String input : nodeInputs.get(t)) {
                level.add(input);
            }

        }
        return level;
    }
}


