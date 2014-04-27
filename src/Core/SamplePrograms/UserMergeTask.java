package Core.SamplePrograms;

import CommLayer.Messages.AvalancheMessages.Message;
import Core.Impl.MergeTaskImpl;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by linmichaelj on 1/7/2014.
 */
public class UserMergeTask extends MergeTaskImpl {
    @Override
    public List<Message.DataPair> userFunction(List<Message.DataPair> input, List<Message.DataPair> prevOutput) {
        List<Message.DataPair> dataPairList = new LinkedList<Message.DataPair>();
        int i = 0;
        int j = 0;
        while(i < input.size() && j < prevOutput.size()){
            int compareTo = input.get(i).getKey().compareTo(prevOutput.get(j).getKey());
            if(compareTo == 0){
                Message.DataPair.Builder dataPair = Message.DataPair.newBuilder();
                dataPair.setKey(input.get(i).getKey());
                dataPair.setValue(String.valueOf(Integer.parseInt(input.get(i).getValue()) + Integer.parseInt(prevOutput.get(j).getValue())));
                dataPairList.add(dataPair.build());
                i ++;
                j ++;
            } else if (compareTo < 0) {
                dataPairList.add(input.get(i));
                i ++;
            } else { //compare To > 0
                dataPairList.add(prevOutput.get(j));
                j++;
            }
        }
        dataPairList.addAll(input.subList(i, input.size()));
        dataPairList.addAll(prevOutput.subList(j, prevOutput.size()));
        return dataPairList;
    }
}
