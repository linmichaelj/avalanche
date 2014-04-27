package Core.Interfaces;

import CommLayer.CommClient;

/**
 * Created by linmichaelj on 2014-03-24.
 */
public interface CopyTask extends Task {
    public void init(String id, CommClient localSchedulerClient, CommClient localDataHandlerClient, CommClient remoteDataHandlerClient, String taskId);
}
