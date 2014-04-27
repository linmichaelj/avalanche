package DataLayer.Test;

import CommLayer.Messages.AvalancheMessages;
import CommLayer.Messages.MessageBuilderUtil;
import Util.ConfigProperties;

import java.io.*;
import java.net.Socket;

/**
 * Created by linmichaelj on 2014-03-26.
 */
public class DataClient {

    public static void main(String [] args ) throws IOException, ClassNotFoundException {
        if(args.length == 0) return;

        String fileName = ConfigProperties.getProperty("LAST_QUERY_FILE");

        PrintWriter writer = new PrintWriter(new File(fileName));
        writer.print("");
        writer.close();
        writer = new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)));


        Socket socket = new Socket(ConfigProperties.getProperty(("HOST")), ConfigProperties.getIntProperty("DH_PORT"));
        OutputStream outputStream = socket.getOutputStream();
        InputStream inputStream = socket.getInputStream();

        MessageBuilderUtil.generateDataHandlerReadMessage(args[0], 0, Integer.MAX_VALUE).build().writeDelimitedTo(outputStream);

        AvalancheMessages.Message message = AvalancheMessages.Message.parseDelimitedFrom(inputStream);
        System.out.println("Received Message of type" + message.getMessageType());


        AvalancheMessages.Message.DataHandlerMessage innerMessage = message.getDataHandlerMessage();
        if(innerMessage.getType() == AvalancheMessages.Message.DataHandlerMessage.Type.READ_RESPONSE){
            System.out.println(innerMessage.getDataPayLoadList());
            writer.println(innerMessage.getDataPayLoadList());
        }
        writer.flush();
        writer.close();

        MessageBuilderUtil.generateByeMessage().build().writeDelimitedTo(outputStream);
        outputStream.flush();
        outputStream.close();
    }
}
