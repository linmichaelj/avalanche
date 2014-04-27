package CommLayer;


import CommLayer.Messages.AvalancheMessages.Message;
import CommLayer.Messages.MessageBuilderUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Observable;

/**
 * Created by linmichaelj on 3/7/2014.
 */
public class StreamThread extends Observable implements Runnable {
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean reading;
    private String ownerId;
    private boolean debug = false;

    public StreamThread(Socket socket, String ownerId) throws IOException {
        this.socket = socket;
        this.socket.setSoTimeout(1000);
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
        reading = true;
        this.ownerId = ownerId;
        System.out.println("[" + ownerId + ": StreamThread]. Created");
    }

    public void run(){
        while(reading){
            try {
                Message message = Message.parseDelimitedFrom(inputStream);
                if(message == null){
                    reading = false;
                    break;
                }
                setChanged();
                notifyObservers(message);

                if(message.getMessageType() != Message.MessageType.HEART_BEAT_MESSAGE && debug) {
                    System.out.println("[" + ownerId + " StreamThread] Received: \n" + message);
                }

                if(message.getMessageType() == Message.MessageType.BYE) {
                    reading = false;
                    break;
                }

            } catch (IOException e) {
                //e.printStackTrace();
            }
        }

        System.out.println("[" + ownerId + " StreamThread]. Exiting");
        try {
            inputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void sendMessage(Message.Builder message){
        if(outputStream != null){
            try {

                message.setSenderId(ownerId);
                Message builtMessage = message.build();
                builtMessage.writeDelimitedTo(outputStream);
                if(builtMessage.getMessageType() != Message.MessageType.HEART_BEAT_MESSAGE && debug) {
                    System.out.println("[" + ownerId + " StreamThread] Sending: \n" + builtMessage);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void stop(){
        if(outputStream != null){
            try {
                Message.Builder byeMessage = MessageBuilderUtil.generateByeMessage();
                byeMessage.setSenderId(ownerId);
                byeMessage.build().writeDelimitedTo(outputStream);
                outputStream.flush();
                outputStream.close();
                reading = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}