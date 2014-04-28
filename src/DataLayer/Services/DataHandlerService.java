package DataLayer.Services;

import CommLayer.CommClient;
import CommLayer.CommServer;
import CommLayer.HeartBeatGenerator;
import CommLayer.Messages.AvalancheMessages.Message;
import CommLayer.Messages.MessageBuilderUtil;
import CommLayer.StreamThread;
import DataLayer.Impl.DataHandlerImpl;
import DataLayer.Interfaces.DataHandler;
import Util.ConfigProperties;

import java.util.Observable;
import java.util.Observer;

/**
 * Created by linmichaelj on 2014-03-22.
 */
public class DataHandlerService implements Observer {

    public CommServer server;
    private DataHandler dh;
    public CommClient dataManagerClient;
    private String host;
    private String dmHost;
    private int tsPort;
    private int dhPort;
    private int lsPort;
    private int dmPort;

    public DataHandlerService(){
        loadConfigProperties();
        dh = new DataHandlerImpl();
        dataManagerClient = new CommClient("DataManagerClient", dmHost, dmPort);
        dataManagerClient.addInputObserver(this);

        new HeartBeatGenerator(new CommClient("DH HBM", dmHost, dmPort));

        server = new CommServer(ConfigProperties.getIntProperty("DH_PORT"), "DataHandlerServer");
        server.addInputObserver(this);
        new Thread(server).start();
    }

    public void loadConfigProperties(){
        host = ConfigProperties.getProperty("HOST");
        tsPort = ConfigProperties.getIntProperty("TS_PORT");
        dhPort = ConfigProperties.getIntProperty("DH_PORT");
        lsPort = ConfigProperties.getIntProperty("LS_PORT");
        dmHost = ConfigProperties.getProperty("DM_HOST");
        dmPort = ConfigProperties.getIntProperty("DM_PORT");
    }

    @Override
    public void update(Observable observable, Object o) {
        Message message;
        if((message = MessageBuilderUtil.getMessageOfType(o, Message.MessageType.DATA_HANDLER_MESSAGE)) != null){
            Message.DataHandlerMessage innerMessage = message.getDataHandlerMessage();
            if(innerMessage.getType() == Message.DataHandlerMessage.Type.READ_REQ){
                StreamThread streamThread = (StreamThread) observable;
                Message.DataHandlerMessage.MetaData.Builder metaData = Message.DataHandlerMessage.MetaData.newBuilder();
                streamThread.sendMessage(MessageBuilderUtil.generateDataHandlerReadResponseMessage(innerMessage.getTaskId(), dh.getOutput(innerMessage.getTaskId(), innerMessage.getReadMinRange(), innerMessage.getReadMaxRange(), metaData), metaData.build()));
            }

            if(innerMessage.getType() == Message.DataHandlerMessage.Type.WRITE){
                dh.writeOutput(innerMessage.getDataPayLoadList(), innerMessage.getTaskId(), innerMessage.getMetaData().getDataPairList());
                if(innerMessage.getMetaData().getIsComplete()) {
                    dataManagerClient.sendMessage(MessageBuilderUtil.generateDataManagerRegMessage(innerMessage.getTaskId(), host, tsPort, host, dhPort));
                }
            }
        }
    }

    public static void main(String [] args){
        new DataHandlerService();
    }
}
