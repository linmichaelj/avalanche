package DataLayer.Test;

import CommLayer.Messages.AvalancheMessages.Message;
import CommLayer.Messages.MessageBuilderUtil;
import Util.ConfigProperties;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by linmichaelj on 2014-03-22.
 */
public class DataHandlerTest {

    public static void main(String [] args){
        Socket socket = null;
        try {
            socket = new Socket(ConfigProperties.getProperty("HOST"), ConfigProperties.getIntProperty("DH_PORT"));
            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream();

            List<Message.DataPair> dataPairList = new LinkedList<Message.DataPair>();

            Message.DataPair.Builder dataPair = Message.DataPair.newBuilder();
            dataPair.setKey("a");
            dataPair.setValue("1");
            dataPairList.add(dataPair.build());

            dataPair.setKey("b");
            dataPair.setValue("2");
            dataPairList.add(dataPair.build());

            dataPair.setKey("c");
            dataPair.setValue("3");
            dataPairList.add(dataPair.build());

            MessageBuilderUtil.generateDataHandlerWriteMessage("DataHandler Test", dataPairList, generateMetaData(), false).build().writeDelimitedTo(outputStream);

            MessageBuilderUtil.generateDataHandlerReadMessage("DataHandler Test", 0, Integer.MAX_VALUE).build().writeDelimitedTo(outputStream);

            Message message = Message.parseDelimitedFrom(inputStream);
            System.out.println("Received Message of type" + message.getMessageType());

            Message.DataHandlerMessage innerMessage = message.getDataHandlerMessage();
            if(innerMessage.getType() == Message.DataHandlerMessage.Type.READ_RESPONSE){
                System.out.println(innerMessage.getDataPayLoadList());
            }

            MessageBuilderUtil.generateDataHandlerReadMessage("DataHandler Test", 1, 1).build().writeDelimitedTo(outputStream);

            message = Message.parseDelimitedFrom(inputStream);
            System.out.println("Received Message of type" + message.getMessageType());

            innerMessage = message.getDataHandlerMessage();
            if(innerMessage.getType() == Message.DataHandlerMessage.Type.READ_RESPONSE){
                System.out.println(innerMessage.getDataPayLoadList());
            }

            MessageBuilderUtil.generateByeMessage().build().writeDelimitedTo(outputStream);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<Message.DataPair> generateMetaData(){
        List<Message.DataPair> metaData = new LinkedList<Message.DataPair>();
        metaData.add(Message.DataPair.newBuilder().setKey("META1").setValue("VALUE1").build());
        metaData.add(Message.DataPair.newBuilder().setKey("META2").setValue("VALUE2").build());

        return metaData;
    }
}
