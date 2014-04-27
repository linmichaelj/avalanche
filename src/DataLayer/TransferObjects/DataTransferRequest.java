package DataLayer.TransferObjects;

import CommLayer.Messages.AvalancheMessages.Message;

/**
 * Created by linmichaelj on 1/30/2014.
 */
public class DataTransferRequest {
    private String taskId;
    private Message.Location destDataHandlerLocation;

    public DataTransferRequest(String taskId, Message.Location destDataHandlerLocation){
        this.taskId = taskId;
        this.destDataHandlerLocation = destDataHandlerLocation;
    }

    public String getTaskId(){
        return taskId;
    }

    public Message.Location getDestDataHandlerLocation(){
        return destDataHandlerLocation;
    }

    @Override
    public boolean equals(Object obj){
        if(obj == null) return false;
        if(obj instanceof DataTransferRequest){
            DataTransferRequest request = (DataTransferRequest) obj;
            if(request.getTaskId().equals(taskId) && request.getDestDataHandlerLocation().equals(destDataHandlerLocation)){
                return true;
            }
        }
        return false;
    }

    public int hashCode(){
        return toString().hashCode();
    }

    public String toString(){
        return "DataTransferRequest: " + taskId + ", " + destDataHandlerLocation.getHost() + ": " + destDataHandlerLocation.getPort();
    }
}