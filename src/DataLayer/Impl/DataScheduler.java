package DataLayer.Impl;

import CommLayer.Messages.AvalancheMessages.Message;
import CommLayer.Messages.MessageBuilderUtil;
import DataLayer.TransferObjects.DataTransferRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Observable;

/**
 * Created by linmichaelj on 2/26/14.
 */
public class DataScheduler extends Observable implements Runnable {
    private LinkedList<DataTransferRequest> blockedRequests;
    private LinkedList<DataTransferRequest> readyRequests;
    private LinkedList<DataTransferRequest> runningRequests;
    private LinkedList<LinkedList<DataTransferRequest>> requests;

    private DataRegistry reg;

    public DataScheduler(DataRegistry reg){
        this.reg = reg;
        blockedRequests = new LinkedList<DataTransferRequest>();
        readyRequests = new LinkedList<DataTransferRequest>();
        runningRequests = new LinkedList<DataTransferRequest>();
        requests = new LinkedList<LinkedList<DataTransferRequest>>();
        requests.add(blockedRequests);
        requests.add(readyRequests);
        requests.add(runningRequests);
    }

    private synchronized void addBlockedRequest(DataTransferRequest request){
        if(blockedRequests.contains(request)) return;
        System.out.println("Added Request for " + request.getTaskId()  + " to Blocked Requests");
        blockedRequests.add(request);
    }

    private synchronized void readyRequest(DataTransferRequest request){
        if(readyRequests.contains(request)) return;
        System.out.println("Added Request for " + request.getTaskId()  + " to Ready Requests");
        if(blockedRequests.contains(request)){
            blockedRequests.remove(request);
            readyRequests.add(request);
        }
    }

    private synchronized void readyRequests(String taskId){
        LinkedList<DataTransferRequest> toReady = new LinkedList<DataTransferRequest>();
        for(DataTransferRequest request: blockedRequests){
            if(request.getTaskId().equals(taskId)){
                toReady.add(request);
            }
        }
        for(DataTransferRequest request: toReady){
            readyRequest(request);
        }
    }

    private synchronized void runRequest(DataTransferRequest request){
        System.out.println("Added Request for " + request.getTaskId()  + " to Run Requests");

        if(readyRequests.contains(request)){
            readyRequests.remove(request);
            runningRequests.add(request);
        }
    }

    public synchronized void markRequestComplete(DataTransferRequest request){
        for(LinkedList<DataTransferRequest> requestList: requests){
            if(requestList.contains(request)){
                System.out.println("Request for " + request.getTaskId()  + " marked as complete");
                requestList.remove(request);
                setChanged();
                notifyObservers(request);
            }
        }
    }


    public void scheduleDataTransfer(DataTransferRequest request) {
        addBlockedRequest(request);
        if(reg.getDataHandlerLocations(request.getTaskId()) != null && reg.getDataHandlerLocations(request.getTaskId()).contains(request.getDestDataHandlerLocation())){
            System.out.println("REQUEST FOR TRANSFER IS ALREADY GOOD TO GO");
            markRequestComplete(request);
        }

        LinkedList<Message.Location> sourceTaskInitLocations = reg.getTaskInitLocations(request.getTaskId());
        if(sourceTaskInitLocations != null && sourceTaskInitLocations.size() > 0){
            readyRequest(request);
        }
    }

    public void notifyNewDataSource(String taskId, Message.Location dataHandlerLocation) {
        System.out.println("Notified of new data source for " + taskId);
        readyRequests(taskId);
        markRequestComplete(new DataTransferRequest(taskId, dataHandlerLocation));
    }

    //Based on FIFO
    private synchronized DataTransferRequest runNextScheduledTransfer(){
        if(readyRequests.size() > 0) {
            DataTransferRequest request = readyRequests.getFirst();
            runRequest(request);
            return request;
        } else {
            return null;
        }
    }

    public void run() {
        while(!Thread.currentThread().isInterrupted()){
            if(runningRequests.size() == 1){
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if(runningRequests.size() == 0){
                DataTransferRequest request;
                if((request = runNextScheduledTransfer()) != null){
                    try {
                        executeDataTransferRequest(request);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void executeDataTransferRequest(DataTransferRequest request) throws IOException {
        System.out.println("Execute Transfer Request " + request.getTaskId());
        Message.Location taskServerLoc = reg.getTaskInitLocations(request.getTaskId()).get(0);

        Socket socket = new Socket(taskServerLoc.getHost(), taskServerLoc.getPort());
        OutputStream outputStream = socket.getOutputStream();
        Message.Location destLocation = request.getDestDataHandlerLocation();
        MessageBuilderUtil.generateCopyInitMessage(request.getTaskId(), destLocation.getHost(), destLocation.getPort()).build().writeDelimitedTo(outputStream);
        MessageBuilderUtil.generateByeMessage().build().writeDelimitedTo(outputStream);
    }
}
