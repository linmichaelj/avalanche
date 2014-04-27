package Core.Test;

import CommLayer.Messages.AvalancheMessages.Message;
import CommLayer.Messages.MessageBuilderUtil;
import Util.ConfigProperties;

import java.io.*;
import java.net.Socket;

/**
 * Created by linmichaelj on 3/5/2014.
 */
public class BuilderTest {

    public static void main(String [] args) throws Exception {
        BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));

        String host = ConfigProperties.getProperty("HOST");
        Socket socket = new Socket(host, ConfigProperties.getIntProperty("TS_PORT"));
        OutputStream outputStream = socket.getOutputStream();

        Message.Builder initTaskMessage = MessageBuilderUtil.generateBuilderInitMessage(
                "Builder2", "raw-data");

        initTaskMessage.build().writeDelimitedTo(outputStream);
        MessageBuilderUtil.generateByeMessage().build().writeDelimitedTo(outputStream);
        outputStream.flush();
        outputStream.close();

        socket = new Socket(host, ConfigProperties.getIntProperty("DH_PORT"));
        outputStream = socket.getOutputStream();
        InputStream inputStream = socket.getInputStream();

        System.out.println("Press Enter To Read Output");
        try {
            bufferRead.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        MessageBuilderUtil.generateDataHandlerReadMessage("Builder2", 0, Integer.MAX_VALUE).build().writeDelimitedTo(outputStream);

        Message message = Message.parseDelimitedFrom(inputStream);
        System.out.println("Received Message of type " + message.getMessageType());

        Message.DataHandlerMessage innerMessage = message.getDataHandlerMessage();

        if(innerMessage.getType() == Message.DataHandlerMessage.Type.READ_RESPONSE){
            System.out.println(innerMessage.getDataPayLoadList());
        }
    }
}