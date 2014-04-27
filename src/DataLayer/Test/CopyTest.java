package DataLayer.Test;

import CommLayer.Messages.AvalancheMessages.Message;
import CommLayer.Messages.MessageBuilderUtil;
import Util.ConfigProperties;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by linmichaelj on 2014-03-23.
 */
public class CopyTest {

    public static void main(String [] args) throws Exception {
        //Connect to DataManager
        Socket socket = new Socket(ConfigProperties.getProperty(("DM_HOST")), ConfigProperties.getIntProperty("DM_PORT"));
        OutputStream outputStream = socket.getOutputStream();
        InputStream inputStream = socket.getInputStream();

        MessageBuilderUtil.generateDataManagerDataCopyRequestMessage("Builder 3", ConfigProperties.getProperty(("HOST")), ConfigProperties.getIntProperty("DH_PORT")).build().writeDelimitedTo(outputStream);

        Message message = Message.parseDelimitedFrom(inputStream);

        System.out.println("Received " + message.getMessageType());

        MessageBuilderUtil.generateByeMessage().build().writeDelimitedTo(outputStream);
        outputStream.flush();
        outputStream.close();
    }
}
