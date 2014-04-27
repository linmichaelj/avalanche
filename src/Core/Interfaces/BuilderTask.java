package Core.Interfaces;

import CommLayer.CommClient;
import CommLayer.Messages.AvalancheMessages.Message;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by linmichaelj on 12/24/13.
 */
public interface BuilderTask <M,N> extends Task {
    public void init(String id, String indexPath, CommClient localScedulerClient, CommClient dataHandlerClient, CommClient heartBeatClient);

    public HashMap<M,N> userFunction(LinkedList<String> input, HashMap<M,N> prevOutput);

    public List<Message.DataPair> convertMapToProtoBuf(HashMap<M,N> map);

    public HashMap<M,N> convertProtoBufToMap(List<Message.DataPair> dataPairList);
}
