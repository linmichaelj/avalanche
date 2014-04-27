package DataLayer.Interfaces;

import CommLayer.Messages.AvalancheMessages.Message;

import java.util.List;

/**
 * Created by linmichaelj on 1/22/2014.
 */
public interface DataHandler {

    public List<Message.DataPair> getOutput(String taskId, int minRange, int maxRange, Message.DataHandlerMessage.MetaData.Builder retrievedMetaData);

    public void writeOutput(List<Message.DataPair> output, String taskId, List<Message.DataPair> metaDataPairs);

}
