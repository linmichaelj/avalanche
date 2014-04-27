package Core.Impl;

import CommLayer.CommClient;
import CommLayer.Messages.AvalancheMessages.Message;
import CommLayer.Messages.MessageBuilderUtil;
import Core.Interfaces.MergeTask;
import Core.Status;
import Util.ConfigProperties;

import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by linmichaelj on 2014-03-24.
 */
public abstract class MergeTaskImpl implements MergeTask {
    private Status status;
    private String id;
    private CommClient statusClient;
    private CommClient dataHandlerClient;
    private CommClient dataManagerClient;
//    private CommClient heartBeatClient;
    private List<String> pendingDataSources;
    private BlockingQueue<String> readyDataSources;
    private BlockingQueue<String> completeDataSources;

    private List<Message.DataPair> input;
    private boolean pauseFlag;
    private List<Message.DataPair> output;
//    private HeartBeatGenerator heartBeatGenerator;

    public enum MetaDataType{COMPLETE_DATA_SOURCE, CURRENT_SOURCE, CURRENT_INDEX};
    public String currentSource;
    public int currentIndex;
    public int MERGE_MAX_INPUT;
    public int SNAPSHOT_PERIOD;

    public boolean running;
    public boolean requestArrived;

    public MergeTaskImpl(){
        MERGE_MAX_INPUT = ConfigProperties.getIntProperty("MERGE_MAX_INPUT");
        SNAPSHOT_PERIOD = ConfigProperties.getIntProperty("SNAPSHOT_PERIOD");

    }

    public void init(String id, CommClient localSchedulerClient, CommClient dataHandlerClient, CommClient dataManagerClient, List<String> pendingDataSources, CommClient heartBeatClient){
        this.id = id;
        status = new Status(id);
        this.statusClient = localSchedulerClient;
        this.dataHandlerClient = dataHandlerClient;
        this.dataManagerClient = dataManagerClient;
//        this.heartBeatClient = heartBeatClient;
        this.pendingDataSources = new LinkedList<String>(); //Necessary because Protobufs implement lazy strings
        this.pendingDataSources.addAll(pendingDataSources);
        completeDataSources = new LinkedBlockingQueue<String>();
        readyDataSources = new LinkedBlockingQueue<String>();
        pauseFlag = false;
        output = new LinkedList<Message.DataPair>();
        input = new LinkedList<Message.DataPair>();
//        heartBeatGenerator = new HeartBeatGenerator(heartBeatClient);
        currentIndex = 0;
        requestArrived = false;
        changeState(Status.State.LOADING);
        fetchDependencies();
    }

    public void fetchDependencies(){
        for(String source: pendingDataSources){
            dataManagerClient.sendMessage(MessageBuilderUtil.generateDataManagerDataCopyRequestMessage(source, dataHandlerClient.getHost(), dataHandlerClient.getPort()));
        }
    }

    private void changeState(Status.State state){
        status.setState(state);
        statusClient.sendMessage(MessageBuilderUtil.generateTaskStatusMessage(status.getId(), status.getState()));
    }

    @Override
    public void update(Observable o, Object arg) {

        Message message;
        if((message = MessageBuilderUtil.getMessageOfType(arg, Message.MessageType.TASK_SET_STATE_MESSAGE)) != null){
            if(message.getTaskSetStateMessage().getState() == Message.TaskSetStateMessage.State.RUN){
                if(status.getState() == Status.State.READY) {
                    dataHandlerClient.sendMessage(MessageBuilderUtil.generateDataHandlerReadMessage(id, 0, Integer.MAX_VALUE));
                    changeState(Status.State.RECOVERY);
                }
            } else if (message.getTaskSetStateMessage().getState() == Message.TaskSetStateMessage.State.PAUSE) {
                if(status.getState() == Status.State.RUNNING){
                    pauseFlag = true;
                }
            } else if (message.getTaskSetStateMessage().getState() == Message.TaskSetStateMessage.State.RESUME) {
                if(status.getState() == Status.State.INTERRUPTED){
                    dataHandlerClient.sendMessage(MessageBuilderUtil.generateDataHandlerReadMessage(id, 0, Integer.MAX_VALUE));
                }
            }
        }

        else if((message = MessageBuilderUtil.getMessageOfType(arg, Message.MessageType.DATA_MANAGER_MESSAGE)) != null){
            Message.DataManagerMessage innerMessage = message.getDataManagerMessage();
            if(innerMessage.getType() == Message.DataManagerMessage.Type.DATA_COPY_RESPONSE){
                readyDataSources.add(innerMessage.getDataCopyResponseMessage().getTaskId());
                pendingDataSources.remove(innerMessage.getDataCopyResponseMessage().getTaskId());
                if(pendingDataSources.size() == 0){
                    changeState(Status.State.READY);
                }
            }
        }

        else if ((message = MessageBuilderUtil.getMessageOfType(arg, Message.MessageType.DATA_HANDLER_MESSAGE)) != null){
            Message.DataHandlerMessage innerMessage = message.getDataHandlerMessage();
            System.out.println("LOADING " + innerMessage);
            if(innerMessage.getType() == Message.DataHandlerMessage.Type.READ_RESPONSE && innerMessage.getTaskId().equals(id)){
                output.addAll(innerMessage.getDataPayLoadList());
                for(Message.DataPair dataPair: innerMessage.getMetaData().getDataPairList()){
                    if(dataPair.getKey().equals(MetaDataType.CURRENT_INDEX.toString())){
                        currentIndex = Integer.parseInt(dataPair.getValue());
                    } else if(dataPair.getKey().equals(MetaDataType.CURRENT_SOURCE.toString())){
                        currentSource = dataPair.getValue();
                    } else if(dataPair.getKey().length() > MetaDataType.COMPLETE_DATA_SOURCE.toString().length()){
                        if(dataPair.getKey().substring(0, MetaDataType.COMPLETE_DATA_SOURCE.toString().length()).equals(MetaDataType.COMPLETE_DATA_SOURCE.toString())) {
                            readyDataSources.remove(dataPair.getValue());
                            completeDataSources.add(dataPair.getValue());
                        }
                    }
                }
                running = requestNextInput();
                pauseFlag = false;
                changeState(Status.State.RUNNING);
            } else if(innerMessage.getType() == Message.DataHandlerMessage.Type.READ_RESPONSE){
                if(pauseFlag){
                    requestArrived = true;
                    System.out.println("REQUEST ARRIVE FLAG");
                }
                else {
                    if (innerMessage.getDataPayLoadCount() == 0) {
                        readyDataSources.remove(innerMessage.getTaskId());
                        completeDataSources.add(innerMessage.getTaskId());
                        currentIndex = 0;
                        System.out.println("ADD " + innerMessage.getTaskId() +  " to complete");
                        running = requestNextInput();
                    } else {
                        input = innerMessage.getDataPayLoadList();
                        currentSource = innerMessage.getTaskId();
                    }
                }
            }
        }

        else {
            System.out.println("Task " + id + "]. received unknown message of type ");
        }
    }

    @Override
    public void run() {
        while(status.getState() != Status.State.RUNNING){
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        (new Thread(){
            public void run() {
                while (status.getState() != Status.State.FINISHED) {
                    try {
                        Thread.sleep(SNAPSHOT_PERIOD);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(!pauseFlag) {
                        synchronized (output) {
                            dataHandlerClient.sendMessage(MessageBuilderUtil.generateDataHandlerWriteMessage(id, output, generateMetaData(), false));
                        }
                    }
                }
            }
        }).start();


        running = requestNextInput();
        while(running && readyDataSources.size() > 0 || (input != null && input.size() > 0)){
            if(input != null && input.size() > 0){
                synchronized (output) {
                    System.out.println("READING CHUNK OF SIZE " + input.size() + " FROM " + currentSource + " " + input);
                    output = userFunction(input, output);
                    currentIndex += input.size();
                    input = null;
                    running = requestNextInput();
                    currentSource = null;
                }
            } else {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("NO INPUT");
            }

            System.out.println("LOOPING");

            if(pauseFlag) {
                dataHandlerClient.sendMessage(MessageBuilderUtil.generateDataHandlerWriteMessage(id, output, generateMetaData(), false));
                System.out.println("PAUSING");
                output = new LinkedList<Message.DataPair>();
                input = new LinkedList<Message.DataPair>();

                changeState(Status.State.INTERRUPTED);

                while(pauseFlag){
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if(requestArrived){
                    running = requestNextInput();
                    requestArrived = false;
                }
            }
        }

        dataHandlerClient.sendMessage(MessageBuilderUtil.generateDataHandlerWriteMessage(id, output, generateMetaData(), true));
        changeState(Status.State.FINISHED);
        //heartBeatGenerator.stopHeartBeatGenerator();
        //heartBeatClient.stop();
        statusClient.stop();
    }

    private boolean requestNextInput(){
        System.out.println("PULLING FROM " + readyDataSources);
        String source = readyDataSources.peek();
        if(source != null) {
            dataHandlerClient.sendMessage(MessageBuilderUtil.generateDataHandlerReadMessage(source, currentIndex, currentIndex+MERGE_MAX_INPUT-1));
            return true;
        }
        return false;
    }

    private List<Message.DataPair> generateMetaData(){
        List<Message.DataPair> metaData = new LinkedList<Message.DataPair>();

        int i = 0;
        for(String source: completeDataSources){
            metaData.add(Message.DataPair.newBuilder().setKey(MetaDataType.COMPLETE_DATA_SOURCE.toString() + i).setValue(source).build());
            i++;
        }

        if(currentSource != null) {
            metaData.add(Message.DataPair.newBuilder().setKey(MetaDataType.CURRENT_SOURCE.toString()).setValue(currentSource).build());
            metaData.add(Message.DataPair.newBuilder().setKey(MetaDataType.CURRENT_INDEX.toString()).setValue(String.valueOf(currentIndex)).build());
        }

        return metaData;
    }
}