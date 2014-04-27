package SchedLayer.UI;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * Created by tomwu on 05/03/14.
 */
public class MachinePool {
    private String configFilename;

    private HashSet<String> machines;

    public MachinePool (String configFilename) {
        this.configFilename = configFilename;
        this.machines = new HashSet<String>();
        this.loadMachinesFromFile();
    }

    public MachinePool (HashSet<String> machines) {
        this.machines = machines;
    }

    private void loadMachinesFromFile() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(this.configFilename));
            try {
                String line = br.readLine();
                while (line != null) {
                    this.machines.add(line);
                    line = br.readLine();
                }

            } finally {
                br.close();
            }

        } catch (Exception e) {
            System.err.println("SchedulingLayer.UI: could not load the machine ip addresses from GUI");
        }
    }

    public HashSet<String> getMachines() {
        return this.machines;
    }

}

