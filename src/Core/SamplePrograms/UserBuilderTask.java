package Core.SamplePrograms;

import CommLayer.Messages.AvalancheMessages.Message;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by linmichaelj on 1/7/14.
 */
public class UserBuilderTask extends Core.Impl.BuilderTaskImpl <String, Integer> {
    @Override
    public HashMap userFunction(LinkedList<String> input, HashMap<String, Integer> prevOutput) {
        for (String str: input){
            for(String word: str.split(" |\t|\n")){
                if (prevOutput.containsKey(word)){
                    prevOutput.put(word, new Integer(prevOutput.get(word) + 1));
                }
                else {
                    prevOutput.put(word, new Integer(1));
                }
            }
        }
        return prevOutput;
    }

    @Override
    public List<Message.DataPair> convertMapToProtoBuf(HashMap<String, Integer> map) {
        List<Message.DataPair> dataPairList = new LinkedList<Message.DataPair>();
        for(Map.Entry<String, Integer> entry: map.entrySet()){
            dataPairList.add(convertEntryToDataPair(entry));
        }
        return dataPairList;
    }

    private Message.DataPair convertEntryToDataPair(Map.Entry<String, Integer> entry){
        Message.DataPair.Builder dataPair = Message.DataPair.newBuilder();
        dataPair.setKey(entry.getKey());
        dataPair.setValue(String.valueOf(entry.getValue()));
        return dataPair.build();
    }

    @Override
    public HashMap<String, Integer> convertProtoBufToMap(List<Message.DataPair> dataPairList) {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        for(Message.DataPair dataPair: dataPairList){
            map.put(dataPair.getKey(), Integer.parseInt(dataPair.getValue()));
        }
        return map;
    }
}
