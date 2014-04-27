package Core.Test;

import CommLayer.Messages.AvalancheMessages;
import CommLayer.Messages.MessageBuilderUtil;
import Util.ConfigProperties;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.LinkedList;

/**
 * Created by linmichaelj on 2014-03-27.
 */
public class Test4Days {
    public static void main(String [] args) throws IOException, ClassNotFoundException {
        BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
        String host  = ConfigProperties.getProperty("HOST");
        Socket socket = new Socket(host, ConfigProperties.getIntProperty("TS_PORT"));
        OutputStream outputStream = socket.getOutputStream();

        LinkedList<String> dataSources = new LinkedList<String>();
        dataSources.add("Builder1");
        dataSources.add("Builder2");
        AvalancheMessages.Message.Builder initMerger = MessageBuilderUtil.generateMergerInitMessage("Merge1", dataSources);
        initMerger.build().writeDelimitedTo(outputStream);

        dataSources = new LinkedList<String>();
        dataSources.add("Builder3");
        dataSources.add("Builder4");
        initMerger = MessageBuilderUtil.generateMergerInitMessage("Merge2", dataSources);
        initMerger.build().writeDelimitedTo(outputStream);

        dataSources = new LinkedList<String>();
        dataSources.add("Merge1");
        dataSources.add("Merge2");
        initMerger = MessageBuilderUtil.generateMergerInitMessage("Merge3", dataSources);
        initMerger.build().writeDelimitedTo(outputStream);

        System.out.println("Press Enter To Run Builders Output");
        try {
            bufferRead.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        AvalancheMessages.Message initBuilder1 = MessageBuilderUtil.generateBuilderInitMessage("Builder1", "raw-data").build();
        AvalancheMessages.Message initBuilder2 = MessageBuilderUtil.generateBuilderInitMessage("Builder2", "raw-data").build();
        AvalancheMessages.Message initBuilder3 = MessageBuilderUtil.generateBuilderInitMessage("Builder3", "raw-data").build();
        AvalancheMessages.Message initBuilder4 = MessageBuilderUtil.generateBuilderInitMessage("Builder4", "raw-data").build();


        initBuilder1.writeDelimitedTo(outputStream);
        initBuilder2.writeDelimitedTo(outputStream);

        initBuilder3.writeDelimitedTo(outputStream);
        initBuilder4.writeDelimitedTo(outputStream);

        MessageBuilderUtil.generateByeMessage().build().writeDelimitedTo(outputStream);
        outputStream.flush();
        outputStream.close();
    }
}
