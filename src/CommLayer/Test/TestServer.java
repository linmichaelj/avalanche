package CommLayer.Test;

import CommLayer.CommServer;
import CommLayer.Messages.AvalancheMessages;
import CommLayer.Messages.MessageBuilderUtil;
import CommLayer.StreamThread;

import java.util.Observable;
import java.util.Observer;

/**
 * Use this test server to test your client.
 * This test server will receive will receive the incoming connection from any client,
 * prints out its message type, send the same message back to the client, and close
 * its connection.
 */
public class TestServer implements Observer{
    private static final int serverPort = 4000;

    private CommServer server;

    public TestServer() {
        server = new CommServer(serverPort, "Test Server");
        server.addInputObserver(this);
        (new Thread(server)).start();
        System.out.println("Started server");
    }

    @Override
    public void update(Observable observable, Object o) {

        // Print the received message type
        AvalancheMessages.Message message = (AvalancheMessages.Message) o;
        System.out.println("Received the following message type from client: " + message.getMessageType());

        // Respond to client by saying the same message
        StreamThread serverThread = (StreamThread) observable;
        AvalancheMessages.Message.Builder messageBuilder = AvalancheMessages.Message.newBuilder();
        messageBuilder.mergeFrom(message);
        serverThread.sendMessage(messageBuilder);

        // Stop the client's connection
        serverThread.stop();
        System.out.println("Stopped client's connection.");
    }

    public static void main(String [] args){
        new TestServer();
    }

}

