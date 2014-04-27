package CommLayer.Messages;

import CommLayer.Messages.AvalancheMessages.Message;
import Core.Status;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by linmichaelj on 2014-03-22.
 */
public class MessageBuilderUtil {

    public static Message.Builder generateTaskStatusMessage(String id, Status.State state){
        AvalancheMessages.Message.Builder message = AvalancheMessages.Message.newBuilder();
        message.setMessageType(AvalancheMessages.Message.MessageType.TASK_STATUS_MESSAGE);
        AvalancheMessages.Message.TaskStatusMessage.Builder innerMessage = AvalancheMessages.Message.TaskStatusMessage.newBuilder();
        innerMessage.setId(id);
        innerMessage.setStatus(AvalancheMessages.Message.TaskStatusMessage.Status.valueOf(state.toString()));
        message.setTaskStatusMessage(innerMessage);
        return message;
    }

    public static Message.Builder generateBuilderInitMessage(String id, String path){
        Message.Builder message = Message.newBuilder();
        message.setMessageType(Message.MessageType.TASK_INIT_MESSAGE);
        Message.TaskInitMessage.Builder innerMessage = Message.TaskInitMessage.newBuilder();
        innerMessage.setType(Message.TaskInitMessage.TaskType.BUILDER_TASK);
        innerMessage.setId(id);
        innerMessage.addDataSource(path);
        message.setTaskInitMessage(innerMessage);
        return message;
    }


    public static Message.Builder generateMergerInitMessage(String id, LinkedList<String> dataSources){
        Message.Builder message = Message.newBuilder();
        message.setMessageType(Message.MessageType.TASK_INIT_MESSAGE);
        Message.TaskInitMessage.Builder innerMessage = Message.TaskInitMessage.newBuilder();
        innerMessage.setType(Message.TaskInitMessage.TaskType.MERGER_TASK);
        innerMessage.setId(id);
        innerMessage.addAllDataSource(dataSources);
        message.setTaskInitMessage(innerMessage);
        return message;
    }

    public static Message.Builder generateCopyInitMessage(String id, String host, int port){
        Message.Builder message = Message.newBuilder();
        message.setMessageType(Message.MessageType.TASK_INIT_MESSAGE);
        Message.TaskInitMessage.Builder innerMessage = Message.TaskInitMessage.newBuilder();
        innerMessage.setId(id);
        innerMessage.setType(Message.TaskInitMessage.TaskType.COPY_TASK);

        Message.Location.Builder copyDestination = Message.Location.newBuilder();
        copyDestination.setHost(host);
        copyDestination.setPort(port);

        innerMessage.setCopyDestination(copyDestination);
        message.setTaskInitMessage(innerMessage);
        return message;
    }

    public static Message.Builder generateByeMessage(){
        Message.Builder message = Message.newBuilder();
        message.setMessageType(Message.MessageType.BYE);
        return message;
    }

    public static Message.Builder generateTaskSetStateMessage(String id, Message.TaskSetStateMessage.State state){
        Message.Builder message = Message.newBuilder();
        message.setMessageType(Message.MessageType.TASK_SET_STATE_MESSAGE);

        Message.TaskSetStateMessage.Builder innerMessage = Message.TaskSetStateMessage.newBuilder();
        innerMessage.setId(id);
        innerMessage.setState(state);

        message.setTaskSetStateMessage(innerMessage);
        return message;
    }

    public static Message getMessageOfType(Object o, Message.MessageType type){
        if(o instanceof Message){
            Message message = (Message) o;
            if(message.getMessageType() == type){
                return message;
            }
        }
        return null;
    }

    public static Message.Builder generateDataHandlerReadResponseMessage(String taskId, List<Message.DataPair> dataPairList, Message.DataHandlerMessage.MetaData metaData) {
        Message.Builder message = Message.newBuilder();
        message.setMessageType(Message.MessageType.DATA_HANDLER_MESSAGE);

        Message.DataHandlerMessage.Builder innerMessage = Message.DataHandlerMessage.newBuilder();
        innerMessage.setType(Message.DataHandlerMessage.Type.READ_RESPONSE);
        innerMessage.setTaskId(taskId);
        if(dataPairList != null) innerMessage.addAllDataPayLoad(dataPairList);
        innerMessage.setMetaData(metaData);
        message.setDataHandlerMessage(innerMessage);
        return message;
    }

    public static Message.Builder generateDataHandlerReadMessage(String taskId, int min, int max){
        Message.Builder message = Message.newBuilder();
        message.setMessageType(Message.MessageType.DATA_HANDLER_MESSAGE);

        Message.DataHandlerMessage.Builder innerMessage = Message.DataHandlerMessage.newBuilder();
        innerMessage.setType(Message.DataHandlerMessage.Type.READ_REQ);
        innerMessage.setTaskId(taskId);
        innerMessage.setReadMinRange(min);
        innerMessage.setReadMaxRange(max);

        message.setDataHandlerMessage(innerMessage);
        return message;
    }

    public static Message.Builder generateDataHandlerWriteMessage(String taskId, List<Message.DataPair> dataPairs, List<Message.DataPair> metaDataPairs, boolean isComplete) {
        Message.Builder message = Message.newBuilder();
        message.setMessageType(Message.MessageType.DATA_HANDLER_MESSAGE);

        Message.DataHandlerMessage.Builder innerMessage = Message.DataHandlerMessage.newBuilder();
        innerMessage.setType(Message.DataHandlerMessage.Type.WRITE);

        Message.DataHandlerMessage.MetaData.Builder metaData = Message.DataHandlerMessage.MetaData.newBuilder();
        if(metaDataPairs != null) {
            metaData.addAllDataPair(metaDataPairs);
        }
        metaData.setIsComplete(isComplete);
        innerMessage.setMetaData(metaData);

        innerMessage.setTaskId(taskId);
        innerMessage.addAllDataPayLoad(dataPairs);

        message.setDataHandlerMessage(innerMessage);
        return message;
    }



    public static Message.Builder generateDataManagerRegMessage(String taskId, String taskHost, int taskPort, String handlerHost, int handlerPort){
        Message.Builder message = Message.newBuilder();
        message.setMessageType(Message.MessageType.DATA_MANAGER_MESSAGE);

        Message.DataManagerMessage.Builder innerMessage = Message.DataManagerMessage.newBuilder();
        innerMessage.setType(Message.DataManagerMessage.Type.DATA_REG);

        Message.DataManagerMessage.DataRegMessage.Builder dataRegMessage = Message.DataManagerMessage.DataRegMessage.newBuilder();
        dataRegMessage.setTaskId(taskId);

        Message.Location.Builder taskLocation = Message.Location.newBuilder();
        taskLocation.setHost(taskHost);
        taskLocation.setPort(taskPort);

        dataRegMessage.setTaskInitServiceLocation(taskLocation);

        Message.Location.Builder handlerLocation = Message.Location.newBuilder();
        handlerLocation.setHost(handlerHost);
        handlerLocation.setPort(handlerPort);
        dataRegMessage.setDataHandlerServiceLocation(handlerLocation);

        innerMessage.setDataRegMessage(dataRegMessage);
        message.setDataManagerMessage(innerMessage);
        return message;
    }


    public static Message.Builder generateDataManagerDataSourceRequestMessage(String taskId){
        Message.Builder message = Message.newBuilder();
        message.setMessageType(Message.MessageType.DATA_MANAGER_MESSAGE);

        Message.DataManagerMessage.Builder innerMessage = Message.DataManagerMessage.newBuilder();
        innerMessage.setType(Message.DataManagerMessage.Type.DATA_SOURCE_REQ);

        Message.DataManagerMessage.DataSourceRequestMessage.Builder dataSourceReqMessage = Message.DataManagerMessage.DataSourceRequestMessage.newBuilder();
        dataSourceReqMessage.setTaskId(taskId);

        innerMessage.setDataSourceRequestMessage(dataSourceReqMessage);

        message.setDataManagerMessage(innerMessage);
        return message;
    }

    public static Message.Builder generateDataManagerDataSourceResponseMessage(String taskId, LinkedList<Message.Location> locations){
        Message.Builder message = Message.newBuilder();
        message.setMessageType(Message.MessageType.DATA_MANAGER_MESSAGE);

        Message.DataManagerMessage.Builder innerMessage = Message.DataManagerMessage.newBuilder();
        innerMessage.setType(Message.DataManagerMessage.Type.DATA_SOURCE_RESPONSE);

        Message.DataManagerMessage.DataSourceResponseMessage.Builder dataSourceResponseMessage = Message.DataManagerMessage.DataSourceResponseMessage.newBuilder();
        dataSourceResponseMessage.setTaskId(taskId);

        if(locations != null){
            dataSourceResponseMessage.addAllDataSource(locations);
        }

        innerMessage.setDataSourceResponseMessage(dataSourceResponseMessage);

        message.setDataManagerMessage(innerMessage);
        return message;
    }

    public static Message.Builder generateDataManagerDataCopyRequestMessage(String taskId, String host, int port){
        Message.Builder message = Message.newBuilder();
        message.setMessageType(Message.MessageType.DATA_MANAGER_MESSAGE);

        Message.DataManagerMessage.Builder innerMessage = Message.DataManagerMessage.newBuilder();
        innerMessage.setType(Message.DataManagerMessage.Type.DATA_COPY_REQ);

        Message.DataManagerMessage.DataCopyRequestMessage.Builder dataCopyRequestMessage = Message.DataManagerMessage.DataCopyRequestMessage.newBuilder();
        dataCopyRequestMessage.setTaskId(taskId);

        Message.Location.Builder dataHandlerLocation = Message.Location.newBuilder();
        dataHandlerLocation.setHost(host);
        dataHandlerLocation.setPort(port);

        dataCopyRequestMessage.setDataHandlerServiceLocation(dataHandlerLocation);
        innerMessage.setDataCopyRequestMessage(dataCopyRequestMessage);

        message.setDataManagerMessage(innerMessage);
        return message;
    }


    public static Message.Builder generateDataManagerDataCopyResponseMessage(String taskId){
        Message.Builder message = Message.newBuilder();
        message.setMessageType(Message.MessageType.DATA_MANAGER_MESSAGE);

        Message.DataManagerMessage.Builder innerMessage = Message.DataManagerMessage.newBuilder();
        innerMessage.setType(Message.DataManagerMessage.Type.DATA_COPY_RESPONSE);

        Message.DataManagerMessage.DataCopyResponseMessage.Builder dataCopyResponseMessage = Message.DataManagerMessage.DataCopyResponseMessage.newBuilder();
        dataCopyResponseMessage.setTaskId(taskId);

        innerMessage.setDataCopyResponseMessage(dataCopyResponseMessage);

        message.setDataManagerMessage(innerMessage);
        return message;
    }

    public static Message.Builder generateHeartBeatMessage(int beatInterval){
        Message.Builder message = Message.newBuilder();
        message.setMessageType(Message.MessageType.HEART_BEAT_MESSAGE);

        Message.HeartBeatMessage.Builder innerMessage = Message.HeartBeatMessage.newBuilder();
        innerMessage.setBeatInterval(beatInterval);


        message.setHeartBeatMessage(innerMessage);
        return message;
    }


    public static Message.Builder generateTopologyStatusMessage(LinkedList<Message.TaskStatusMessage> taskStatusMessages) {
        Message.Builder message = Message.newBuilder();
        message.setMessageType(Message.MessageType.TOPOLOGY_STATUS_MESSAGE);
        Message.TopologyStatusMessage.Builder innerMessage = Message.TopologyStatusMessage.newBuilder();
        innerMessage.addAllTaskStatuses(taskStatusMessages);
        message.setTopologyStatusMessage(innerMessage);
        return message;
    }


}
