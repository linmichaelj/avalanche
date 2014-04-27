package CommLayer.Test;

import CommLayer.CommClient;
import CommLayer.CommServer;
import CommLayer.Messages.AvalancheMessages;
import CommLayer.Messages.MessageBuilderUtil;
import CommLayer.StreamThread;

import java.util.Observable;
import java.util.Observer;

/**
 * Customize this test client to test your server.
 * This test client can establish a connection with a server, send a particular message,
 * get response from the server, and print it.
 */
public class TestClient implements Observer{
    private static final String serverHost = "localhost";
    private static final int serverPort = 4000;

    private CommClient client;

    public TestClient() {
        client = new CommClient("Test client", serverHost, serverPort);
        System.out.println("Started client");
        client.addInputObserver(this);
        AvalancheMessages.Message.Builder message = MessageBuilderUtil.generateBuilderInitMessage("test-id", "test-path");
        client.sendMessage(message);
        System.out.println("Send server builder init message");
    }

    @Override
    public void update(Observable observable, Object o) {
        // Print the received message type
        AvalancheMessages.Message message = (AvalancheMessages.Message) o;
        System.out.println("Received the following message type from server: " + message.getMessageType());
    }

    public static void main(String [] args){
        new TestClient();
    }

}
