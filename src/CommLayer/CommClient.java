package CommLayer;

import CommLayer.Messages.AvalancheMessages.Message;

import java.io.IOException;
import java.net.Socket;
import java.util.Observer;

/**
 * Created by linmichaelj on 3/7/2014.
 */
public class CommClient {
    private StreamThread serverThread;
    private String id;
    private String host;
    private int port;

    public CommClient(String id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
        addServerConnection(host, port);
    }

    private void addServerConnection(String host, int port){
        try {
            serverThread = new StreamThread(new Socket(host, port), id);
            new Thread(serverThread).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(Message.Builder message){
        serverThread.sendMessage(message);
    }

    public void addInputObserver(Observer o){
        serverThread.addObserver(o);
    }

    public void stop(){
        serverThread.stop();
    }

    public String getHost(){
        return host;
    }

    public int getPort(){
        return port;
    }
}
