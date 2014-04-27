package DataLayer.Test;

import CommLayer.Messages.AvalancheMessages;
import CommLayer.Messages.MessageBuilderUtil;
import Util.ConfigProperties;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by linmichaelj on 2014-03-23.
 */
public class DataManagerTest {

    public static void main (String [] args){
        Socket socket = null;
        try {
            String host = ConfigProperties.getProperty("HOST");
            socket = new Socket(ConfigProperties.getProperty("DM_HOST"), ConfigProperties.getIntProperty("DM_PORT"));
            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream();

            MessageBuilderUtil.generateDataManagerRegMessage("Builder 1", host, 2000, host, 4001).build().writeDelimitedTo(outputStream);
            MessageBuilderUtil.generateDataManagerRegMessage("Builder 1", host, 3000, host, 4001).build().writeDelimitedTo(outputStream);

            MessageBuilderUtil.generateDataManagerDataSourceRequestMessage("Builder 1").build().writeDelimitedTo(outputStream);
            AvalancheMessages.Message message = AvalancheMessages.Message.parseDelimitedFrom(inputStream);
            printDataSources(message);

            MessageBuilderUtil.generateDataManagerDataSourceRequestMessage("Builder 2").build().writeDelimitedTo(outputStream);
            message = AvalancheMessages.Message.parseDelimitedFrom(inputStream);
            printDataSources(message);



            MessageBuilderUtil.generateByeMessage().build().writeDelimitedTo(outputStream);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printDataSources(AvalancheMessages.Message message){
        AvalancheMessages.Message.DataManagerMessage innerMessage = message.getDataManagerMessage();
        if(innerMessage.getType() == AvalancheMessages.Message.DataManagerMessage.Type.DATA_SOURCE_RESPONSE){
            AvalancheMessages.Message.DataManagerMessage.DataSourceResponseMessage response = innerMessage.getDataSourceResponseMessage();
            System.out.println("Printing Sources for " + response.getTaskId());
            for(AvalancheMessages.Message.Location loc: response.getDataSourceList()){
                System.out.println("Source for " + response.getTaskId() + " found at " + loc.getHost() + ": " + loc.getPort());
            }
        }
    }
}
