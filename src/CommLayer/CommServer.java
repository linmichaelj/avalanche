package CommLayer;

import Util.ConfigProperties;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.LinkedList;
import java.util.Observer;

/**
 * Created by linmichaelj on 3/7/2014.
 */
public class CommServer implements Runnable {
    private ServerSocket serverSocket;
    private LinkedList<Observer> inputObservers;
    private LinkedList<StreamThread> clientStreams;
    private boolean listening;
    private String id;
    private boolean debug;

    public CommServer(int port, String id) {
        inputObservers = new LinkedList<Observer>();
        clientStreams = new LinkedList<StreamThread>();
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        listening = true;
        this.id = id;
        debug = ConfigProperties.getProperty("DEBUG").equals("true");

    }

    public void run(){
        while(listening){
            try {
                StreamThread clientStream = new StreamThread(serverSocket.accept(), id);
                if(debug)
                    System.out.println("[CommServer" + id + "] Received incoming connection");
                for(Observer o: inputObservers){
                    clientStream.addObserver(o);
                }
                clientStreams.add(clientStream);
                new Thread(clientStream).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void addInputObserver(Observer o){
        inputObservers.add(o);
        for(StreamThread streamThread: clientStreams){
            streamThread.addObserver(o);
        }
    }

    public void stopServer(){
        listening = false;
        for(StreamThread readerThread: clientStreams){
            readerThread.stop();
        }

        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}