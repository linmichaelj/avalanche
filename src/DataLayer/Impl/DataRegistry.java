package DataLayer.Impl;


import CommLayer.Messages.AvalancheMessages.Message;
import Util.ConfigProperties;

import java.io.*;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Created by linmichaelj on 1/20/2014.
 */
public class DataRegistry {
    ConcurrentHashMap<String, LinkedList<Message.Location>> taskInitLocationLookup;
    ConcurrentHashMap<String, LinkedList<Message.Location>> dataHandlerLocationLookup;
    String recoveryFilePath;

    public DataRegistry(){
        dataHandlerLocationLookup = new ConcurrentHashMap<String, LinkedList<Message.Location>>();
        taskInitLocationLookup = new ConcurrentHashMap<String, LinkedList<Message.Location>>();
        recoveryFilePath = ConfigProperties.getProperty("RECOVERY_DIRECTORY") + "/DataReg.backup";
        recover();
    }

    public void recover(){
        File f = new File(recoveryFilePath);
        if(f.exists()) {
            try {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(recoveryFilePath));
                taskInitLocationLookup = (ConcurrentHashMap <String, LinkedList<Message.Location>>) ois.readObject();
                dataHandlerLocationLookup = (ConcurrentHashMap <String, LinkedList<Message.Location>>) ois.readObject();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void saveSnapShot(){
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(recoveryFilePath));
            oos.writeObject(taskInitLocationLookup);
            oos.writeObject(dataHandlerLocationLookup);
            oos.flush();
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void registerDataSource(String taskId, Message.Location taskInitServiceLocation, Message.Location dataHandlerServiceLocation) {
        if(taskInitLocationLookup.containsKey(taskId) && dataHandlerLocationLookup.containsKey(taskId)) {
            dataHandlerLocationLookup.get(taskId).add(dataHandlerServiceLocation);
            taskInitLocationLookup.get(taskId).add(taskInitServiceLocation);
        } else if(!taskInitLocationLookup.containsKey(taskId) && !dataHandlerLocationLookup.containsKey(taskId)) {
            LinkedList<Message.Location> taskInitLocations = new LinkedList<Message.Location>();
            LinkedList<Message.Location> dataHandlerLocations = new LinkedList<Message.Location>();
            taskInitLocations.add(taskInitServiceLocation);
            dataHandlerLocations.add(dataHandlerServiceLocation);
            dataHandlerLocationLookup.put(taskId, dataHandlerLocations);
            taskInitLocationLookup.put(taskId, taskInitLocations);
        } else {
            System.out.println("REGISTRY OUT OF SYNC");
        }
        saveSnapShot();
    }

    public LinkedList<Message.Location> getTaskInitLocations(String taskId) {
        if(taskInitLocationLookup.containsKey(taskId)){
            return taskInitLocationLookup.get(taskId);
        }
        return null;
    }

    public LinkedList<Message.Location> getDataHandlerLocations(String taskId){
        if(dataHandlerLocationLookup.containsKey(taskId)){
            return dataHandlerLocationLookup.get(taskId);
        }
        return null;
    }

}