package Core.Impl;

import CommLayer.CommClient;
import CommLayer.HeartBeatGenerator;
import CommLayer.Messages.AvalancheMessages.Message;
import CommLayer.Messages.MessageBuilderUtil;
import Core.Interfaces.BuilderTask;
import Core.Status;
import Util.ConfigProperties;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;

/**
 * Created by linmichaelj on 1/5/14.
 */
public abstract class BuilderTaskImpl <M,N> implements BuilderTask <M,N> {

    private String indexPath;
    private Status status;
    private String id;
    private CommClient statusClient;
    private CommClient dataHandlerClient;
    private CommClient heartBeatClient;
    private boolean pauseFlag;
    private HashMap output;
    private HeartBeatGenerator heartBeatGenerator;
    private enum MetaDataType {CURRENT_FILE, CURRENT_INDEX};
    private int currentFile;
    private int currentIndex;
    public int MAX_READ_BUFFER;
    public int SNAPSHOT_PERIOD;
    private BufferedReader reader;

    public BuilderTaskImpl(){
        MAX_READ_BUFFER = ConfigProperties.getIntProperty("MAX_READ_BUFFER");
        SNAPSHOT_PERIOD = ConfigProperties.getIntProperty("SNAPSHOT_PERIOD");
    }

    public void init(String id, String indexPath, CommClient localSchedulerClient, CommClient dataHandlerClient, CommClient heartBeatClient){
        this.id = id;
        status = new Status(id);
        this.indexPath = indexPath;
        this.statusClient = localSchedulerClient;
        this.dataHandlerClient = dataHandlerClient;
        pauseFlag = false;
        output = new HashMap();
        this.heartBeatClient = heartBeatClient;
//        heartBeatGenerator = new HeartBeatGenerator(heartBeatClient);
        currentIndex = 0;

        try {
            reader = new BufferedReader(new FileReader(new File (indexPath + "/index")));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        changeState(Status.State.LOADING);
        changeState(Status.State.READY);
    }

    //tweets is necessary to avoid the O(n) time incurred if a new linkedlist is generated
    public void readFile(String filePath, LinkedList<String> tweets) throws IOException {
        File file = new File(filePath);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        for(int i = 0; i < currentIndex; i++) reader.readLine();
        String tweet;
        int linesRead = 0;
        while((tweet = reader.readLine()) != null && linesRead < MAX_READ_BUFFER){
            tweets.add(tweet);
            linesRead ++;
        }
        if(tweet == null){
            currentIndex = 0;
            currentFile ++;
        } else {
            currentIndex = linesRead + currentIndex;
        }
        reader.close();
    }

    public void run(){
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
                        storeOutput(output, true);
                    }
                }
            }
        }).start();

        LinkedList <String> tweets = new LinkedList<String>();
        try {
            String file = reader.readLine();
            while(file != null) {
                if(file.equals("index")){
                    file = reader.readLine();
                    currentFile++;
                    continue;
                }
                readFile(indexPath + "/" + file, tweets);
                if(tweets.size() >= MAX_READ_BUFFER){
                    synchronized (output) {
                        userFunction(tweets, output);
                    }
                    tweets = new LinkedList<String>();
                }

                if(pauseFlag){
                    userFunction(tweets, output);
                    storeOutput(output, true);
                    changeState(Status.State.INTERRUPTED);
                    output = new HashMap();
                    tweets = new LinkedList<String>();
                    while(pauseFlag){
                        Thread.sleep(200);
                    }
                }

                if(currentIndex == 0){
                    file = reader.readLine();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        userFunction(tweets, output);
        storeOutput(output, false);
        changeState(Status.State.FINISHED);
//        heartBeatGenerator.stopHeartBeatGenerator();
//        heartBeatClient.stop();
        statusClient.stop();
    }

    private void changeState(Status.State state){
        status.setState(state);
        statusClient.sendMessage(MessageBuilderUtil.generateTaskStatusMessage(status.getId(), status.getState()));
    }

    @Override
    public void update(Observable observable, Object o) {
        Message message;
        if((message = MessageBuilderUtil.getMessageOfType(o, Message.MessageType.TASK_SET_STATE_MESSAGE)) != null){
            if(message.getTaskSetStateMessage().getState() == Message.TaskSetStateMessage.State.RUN){
                if(status.getState() == Status.State.READY) {
                    pauseFlag = false;
                    dataHandlerClient.sendMessage(MessageBuilderUtil.generateDataHandlerReadMessage(id, 0, Integer.MAX_VALUE));
                    changeState(Status.State.RECOVERY);
                }
            } else if (message.getTaskSetStateMessage().getState() == Message.TaskSetStateMessage.State.PAUSE) {
                    if(status.getState() == Status.State.RUNNING){
                        pauseFlag = true;
                    }
            } else if (message.getTaskSetStateMessage().getState() == Message.TaskSetStateMessage.State.RESUME) {
                if(status.getState() == Status.State.INTERRUPTED){
                    changeState(Status.State.RECOVERY);
                    dataHandlerClient.sendMessage(MessageBuilderUtil.generateDataHandlerReadMessage(id, 0, Integer.MAX_VALUE));
                }
            }
        } else if ((message = MessageBuilderUtil.getMessageOfType(o, Message.MessageType.DATA_HANDLER_MESSAGE)) != null){
            Message.DataHandlerMessage innerMessage = message.getDataHandlerMessage();
            if(innerMessage.getType() == Message.DataHandlerMessage.Type.READ_RESPONSE && innerMessage.getTaskId().equals(id)){
                for(Message.DataPair dataPair: innerMessage.getMetaData().getDataPairList()){
                    if(dataPair.getKey().equals(MetaDataType.CURRENT_INDEX.toString())) currentIndex = Integer.parseInt(dataPair.getValue());
                    if(dataPair.getKey().equals(MetaDataType.CURRENT_FILE.toString())) {
                        int f = Integer.parseInt(dataPair.getValue());
                        if(f > currentFile && reader != null) {
                            for (int i = 0; i < f - currentIndex; i++){
                                try {
                                    reader.readLine();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                currentFile = f;
                            }
                        }
                    }
                }
                output.putAll(convertProtoBufToMap(innerMessage.getDataPayLoadList()));
                pauseFlag = false;
                changeState(Status.State.RUNNING);
            }
        }
        else {
            System.out.println("Task " + id + "]. received unknown message");
        }
    }

    public void storeOutput(HashMap output, boolean partialWrite){
        synchronized (output) {
            List<Message.DataPair> metaData = generateMetaData();
            dataHandlerClient.sendMessage(MessageBuilderUtil.generateDataHandlerWriteMessage(id, convertMapToProtoBuf(output), metaData, !partialWrite));
        }
    }

    private List<Message.DataPair> generateMetaData(){
        List<Message.DataPair> metaData = new LinkedList<Message.DataPair>();
        metaData.add(Message.DataPair.newBuilder().setKey(MetaDataType.CURRENT_FILE.toString()).setValue(String.valueOf(currentFile)).build());
        metaData.add(Message.DataPair.newBuilder().setKey(MetaDataType.CURRENT_INDEX.toString()).setValue(String.valueOf(currentIndex)).build());
        return metaData;
    }
}