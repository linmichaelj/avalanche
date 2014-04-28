package Core.Impl;

import CommLayer.CommClient;
import CommLayer.Messages.AvalancheMessages.Message;
import CommLayer.Messages.MessageBuilderUtil;
import Core.Interfaces.CopyTask;
import Core.Status;

import java.util.List;
import java.util.Observable;

/**
 * Created by linmichaelj on 2014-03-24.
 */
public class CopyTaskImpl implements CopyTask {
    private Status status;
    private String id;
    private CommClient statusClient;
    private CommClient remoteDataHandlerClient;
    private List<Message.DataPair> dataToCopy;
    private String taskId;

    public CopyTaskImpl(){};

    public void init(String id, CommClient statusClient, CommClient localDataHandlerClient, CommClient remoteDataHandlerClient, String taskId){
        this.id = id;
        status = new Status(id);
        this.statusClient = statusClient;
        this.remoteDataHandlerClient = remoteDataHandlerClient;
        this.taskId = taskId;
        dataToCopy = null;
        changeState(Status.State.LOADING);
        localDataHandlerClient.sendMessage(MessageBuilderUtil.generateDataHandlerReadMessage(taskId, 0, Integer.MAX_VALUE));
    }

    public void run(){
        remoteDataHandlerClient.sendMessage(MessageBuilderUtil.generateDataHandlerWriteMessage(taskId, dataToCopy, null, true));
        changeState(Status.State.FINISHED);
    }

    private void changeState(Status.State state){
        status.setState(state);
        statusClient.sendMessage(MessageBuilderUtil.generateTaskStatusMessage(status.getId(), status.getState()));
    }

    @Override
    public void update(Observable observable, Object o) {
        Message message;

        if((message = MessageBuilderUtil.getMessageOfType(o, Message.MessageType.DATA_HANDLER_MESSAGE)) != null){
            Message.DataHandlerMessage innerMessage = message.getDataHandlerMessage();
            if(innerMessage.getType() == Message.DataHandlerMessage.Type.READ_RESPONSE){
                dataToCopy = innerMessage.getDataPayLoadList();
                changeState(Status.State.READY);
                changeState(Status.State.RUNNING);
            }
        }

        else {
            System.out.println("Task " + id + "]. received unknown message");
        }
    }
}
