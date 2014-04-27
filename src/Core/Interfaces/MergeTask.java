package Core.Interfaces;

import CommLayer.CommClient;
import CommLayer.Messages.AvalancheMessages.Message;

import java.util.List;

/**
 * Created by linmichaelj on 2014-03-25.
 */
public interface MergeTask extends Task {
    public void init(String id, CommClient localSchedulerClient, CommClient dataHandlerClient, CommClient dataManagerClient, List<String> reqDataSources, CommClient heartBeatClient);

    public List<Message.DataPair> userFunction(List<Message.DataPair> input, List<Message.DataPair> prevOutput);
}
