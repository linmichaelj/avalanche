package DataLayer.Services;

import CommLayer.CommServer;
import CommLayer.HeartBeatMonitor;
import CommLayer.Messages.AvalancheMessages.Message;
import CommLayer.Messages.MessageBuilderUtil;
import CommLayer.StreamThread;
import Core.Test.ComponentFailureTask;
import DataLayer.Impl.DataRegistry;
import DataLayer.Impl.DataScheduler;
import DataLayer.TransferObjects.DataTransferRequest;
import Util.ConfigProperties;

import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by linmichaelj on 1/20/2014.
 */
public class DataManagerService implements Observer {

    private CommServer server;
    private DataRegistry dataRegistry;
    private DataScheduler dataScheduler;

    private ConcurrentHashMap<DataTransferRequest, LinkedList<StreamThread>> pendingTransferStreams;

    public DataManagerService(){
        pendingTransferStreams = new ConcurrentHashMap<DataTransferRequest, LinkedList<StreamThread>>();
        dataRegistry = new DataRegistry();
        dataScheduler = new DataScheduler(dataRegistry);
        dataScheduler.addObserver(this);
        (new Thread(dataScheduler)).start();
        server = new CommServer(ConfigProperties.getIntProperty("DM_PORT"), "DataManagerCommServer");
        server.addInputObserver(new HeartBeatMonitor(ComponentFailureTask.class));
        server.addInputObserver(this);
        (new Thread(server)).start();
    }

    public synchronized void addStreamToPending(DataTransferRequest request, StreamThread streamThread){
        if(pendingTransferStreams.containsKey(request)){
            pendingTransferStreams.get(request).add(streamThread);
        } else {
            LinkedList<StreamThread> streamThreads = new LinkedList<StreamThread>();
            streamThreads.add(streamThread);
            pendingTransferStreams.put(request, streamThreads);
        }
    }

    public synchronized LinkedList<StreamThread> clearPendingStreams(DataTransferRequest request){
        return pendingTransferStreams.remove(request);
    }

    @Override
    public void update(Observable observable, Object o) {
        Message message;
        if((message = MessageBuilderUtil.getMessageOfType(o, Message.MessageType.DATA_MANAGER_MESSAGE)) != null){
            Message.DataManagerMessage dataManagerMessage = message.getDataManagerMessage();

            if(dataManagerMessage.getType() == Message.DataManagerMessage.Type.DATA_REG){
                Message.DataManagerMessage.DataRegMessage regMessage = dataManagerMessage.getDataRegMessage();
                dataRegistry.registerDataSource(regMessage.getTaskId(), regMessage.getTaskInitServiceLocation(), regMessage.getDataHandlerServiceLocation());
                dataScheduler.notifyNewDataSource(regMessage.getTaskId(), regMessage.getDataHandlerServiceLocation());
            }

            else if(dataManagerMessage.getType() == Message.DataManagerMessage.Type.DATA_SOURCE_REQ){
                String taskId = dataManagerMessage.getDataSourceRequestMessage().getTaskId();
                StreamThread streamThread = (StreamThread) observable;
                streamThread.sendMessage(MessageBuilderUtil.generateDataManagerDataSourceResponseMessage(taskId, dataRegistry.getDataHandlerLocations(taskId)));
            }

            else if (dataManagerMessage.getType() == Message.DataManagerMessage.Type.DATA_COPY_REQ){
                Message.DataManagerMessage.DataCopyRequestMessage copyRequestMessage = dataManagerMessage.getDataCopyRequestMessage();
                DataTransferRequest request = new DataTransferRequest(copyRequestMessage.getTaskId(), copyRequestMessage.getDataHandlerServiceLocation());
                addStreamToPending(request, (StreamThread) observable);
                dataScheduler.scheduleDataTransfer(request);
            }

        } else if (o instanceof DataTransferRequest){
            DataTransferRequest request = (DataTransferRequest) o;
            LinkedList<StreamThread> pendingStreams = clearPendingStreams(request);
            for(StreamThread s: pendingStreams) {
                s.sendMessage(MessageBuilderUtil.generateDataManagerDataCopyResponseMessage(request.getTaskId()));
            }
        }
        else if (MessageBuilderUtil.getMessageOfType(o, Message.MessageType.HEART_BEAT_MESSAGE) == null){
            System.out.println("[Datamanager] Cannot Handle the following received message: ");
        }
    }

    public static void main(String [] args){
        new DataManagerService();
    }
}