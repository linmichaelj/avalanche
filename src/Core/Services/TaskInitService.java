package Core.Services;

import CommLayer.CommClient;
import CommLayer.CommServer;
import CommLayer.Messages.AvalancheMessages.Message;
import CommLayer.Messages.MessageBuilderUtil;
import Core.Impl.CopyTaskImpl;
import Core.Interfaces.BuilderTask;
import Core.Interfaces.CopyTask;
import Core.Interfaces.MergeTask;
import Core.SamplePrograms.UserBuilderTask;
import Core.SamplePrograms.UserMergeTask;
import Util.ConfigProperties;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by linmichaelj on 2014-03-07.
 */
public class TaskInitService implements Observer {

    private CommServer server;
    private String host;
    private String dmHost;

    private int tsPort;
    private int lsPort;
    private int dhPort;
    private int dmPort;

    public TaskInitService() throws IOException {
        loadConfigProperties();
        server = new CommServer(tsPort, "TaskCommServer");

        server.addInputObserver(this);
        new Thread(server).start();
    }

    public void loadConfigProperties(){
        host = ConfigProperties.getProperty("HOST");
        dmHost = ConfigProperties.getProperty("DM_HOST");
        tsPort = ConfigProperties.getIntProperty("TS_PORT");
        lsPort = ConfigProperties.getIntProperty("LS_PORT");
        dhPort = ConfigProperties.getIntProperty("DH_PORT");
        dmPort = ConfigProperties.getIntProperty("DM_PORT");
    }


    //TODO change so that UserBuilderTask can be any string specified by the user
    public void generateTask(Message.TaskInitMessage message){
        if(message.getType() == Message.TaskInitMessage.TaskType.BUILDER_TASK){
            if(message.getId() != null && message.getDataSourceCount() > 0){
                String commId = "LS " + message.getId();
                CommClient localSchedulerClient = new CommClient(commId, host, lsPort);

                commId = "DH " + message.getId();
                CommClient dataHandlerClient = new CommClient(commId, host, dhPort);


                commId = "HB " + message.getId();
                CommClient heartBeatClient = new CommClient(commId, host, lsPort);

                //TODO add ServerConnection for Message Queue
                BuilderTask task = new UserBuilderTask();
                localSchedulerClient.addInputObserver(task);
                dataHandlerClient.addInputObserver(task);
                task.init(message.getId(), message.getDataSource(0), localSchedulerClient, dataHandlerClient, heartBeatClient);
                new Thread(task).start();
            }
        } else if(message.getType() == Message.TaskInitMessage.TaskType.COPY_TASK){
            if(message.getId() != null && message.getCopyDestination() != null){
                String commId = "LS COPY";
                CommClient localSchedulerClient = new CommClient(commId, host, lsPort);

                commId = "Local DH COPY";
                CommClient localDataHandlerClient = new CommClient(commId, host, dhPort);

                commId = "Remote DH COPY";
                Message.Location destLocation = message.getCopyDestination();
                CommClient remoteDataHandlerClient = new CommClient(commId, destLocation.getHost(), destLocation.getPort());

                CopyTask task = new CopyTaskImpl();
                task.init("Copy Process", localSchedulerClient, localDataHandlerClient, remoteDataHandlerClient, message.getId());
                localSchedulerClient.addInputObserver(task);
                localDataHandlerClient.addInputObserver(task);
                remoteDataHandlerClient.addInputObserver(task);
                new Thread(task).start();
            }
        } else if (message.getType() == Message.TaskInitMessage.TaskType.MERGER_TASK){
            if(message.getId() != null && message.getDataSourceCount() > 0){
                String commId = "LS " + message.getId();
                CommClient localSchedulerClient = new CommClient(commId, host, lsPort);

                commId = "DH " + message.getId();
                CommClient localDataHandlerClient = new CommClient(commId, host, dhPort);

                commId = "DM " + message.getId();
                CommClient dataManagerClient = new CommClient(commId, dmHost, dmPort);

                commId = "HB " + message.getId();
                CommClient heartBeatClient = new CommClient(commId, host, lsPort);

                MergeTask task = new UserMergeTask();
                localSchedulerClient.addInputObserver(task);
                localDataHandlerClient.addInputObserver(task);
                dataManagerClient.addInputObserver(task);

                task.init(message.getId(), localSchedulerClient, localDataHandlerClient, dataManagerClient, message.getDataSourceList(), heartBeatClient);

                new Thread(task).start();
            }
        }
    }

    @Override
    public void update(Observable observable, Object o) {
        Message message;
        if((message = MessageBuilderUtil.getMessageOfType(o,Message.MessageType.TASK_INIT_MESSAGE)) != null){
            generateTask(message.getTaskInitMessage());
        }
        else{
            System.out.println("[Task Server] Cannot Handle received message ");
        }
    }

    public static void main(String [] args){
        try {
            new TaskInitService();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
