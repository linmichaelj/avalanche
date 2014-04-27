package Core.Test;

import CommLayer.Messages.AvalancheMessages;
import CommLayer.Messages.MessageBuilderUtil;
import Util.ConfigProperties;

import java.io.*;
import java.net.Socket;
import java.util.LinkedList;

/**
 * Created by linmichaelj on 3/25/2014.
 */
public class MergeTest {

    public static void main(String [] args) throws IOException, ClassNotFoundException {
        BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
        String host  = ConfigProperties.getProperty("HOST");
        Socket socket = new Socket(host, ConfigProperties.getIntProperty("TS_PORT"));
        OutputStream outputStream = socket.getOutputStream();


        LinkedList<String> dataSources = new LinkedList<String>();
        dataSources.add("Builder1");
        dataSources.add("Builder2");
        AvalancheMessages.Message initMerger = MessageBuilderUtil.generateMergerInitMessage("Merge", dataSources).build();
        initMerger.writeDelimitedTo(outputStream);

        System.out.println("Press Enter To Run Builders");
        try {
            bufferRead.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        AvalancheMessages.Message initBuilder1 = MessageBuilderUtil.generateBuilderInitMessage("Builder1", "raw-data").build();
        AvalancheMessages.Message initBuilder2 = MessageBuilderUtil.generateBuilderInitMessage("Builder2", "raw-data").build();

        initBuilder1.writeDelimitedTo(outputStream);
        initBuilder2.writeDelimitedTo(outputStream);

        System.out.println("Press Enter To Read Output");
        try {
            bufferRead.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }


        socket = new Socket(host, ConfigProperties.getIntProperty("DH_PORT"));
        outputStream = socket.getOutputStream();
        InputStream inputStream = socket.getInputStream();

        MessageBuilderUtil.generateDataHandlerReadMessage("Merge", 0, Integer.MAX_VALUE).build().writeDelimitedTo(outputStream);
        AvalancheMessages.Message message = AvalancheMessages.Message.parseDelimitedFrom(inputStream);
        System.out.println("Received Message of type " + message.getMessageType());

        AvalancheMessages.Message.DataHandlerMessage innerMessage = message.getDataHandlerMessage();
        System.out.println(innerMessage.getType());
        if(innerMessage.getType() == AvalancheMessages.Message.DataHandlerMessage.Type.READ_RESPONSE){
            System.out.println(innerMessage.getDataPayLoadList());
        }

        MessageBuilderUtil.generateByeMessage().build().writeDelimitedTo(outputStream);
        outputStream.flush();
        outputStream.close();
    }
}
