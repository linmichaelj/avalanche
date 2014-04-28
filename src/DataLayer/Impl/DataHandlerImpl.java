package DataLayer.Impl;

import CommLayer.Messages.AvalancheMessages.Message;
import DataLayer.Interfaces.DataHandler;
import DataLayer.Utils.DbHelper;
import Util.ConfigProperties;
import org.apache.commons.lang.ArrayUtils;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.WriteBatch;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by linmichaelj on 1/20/2014.
 */

public class DataHandlerImpl implements DataHandler {
    private DB db;
    private HashMap<String, byte[]> keySpace;
    private int keyBase;
    private String recoveryFilePath;
    private final String SIZE_KEY = "SIZE";
    private final int keySpaceBits = 4;

    public DataHandlerImpl(){
        recoveryFilePath = ConfigProperties.getProperty("RECOVERY_DIRECTORY") + "/DataHandler.backup";
        keySpace = new HashMap<String, byte []>();
        recover();
        keyBase = keySpace.size();
        try {
            db = DbHelper.getConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void recover(){
        File f = new File(recoveryFilePath);
        if(f.exists()) {
            try {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(recoveryFilePath));
                keySpace = (HashMap<String, byte[]>) ois.readObject();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void saveSnapShot(){
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(recoveryFilePath));
            oos.writeObject(keySpace);
            oos.flush();
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void generateKey(String taskId){
        ByteBuffer b = ByteBuffer.allocate(keySpaceBits);
        b.putInt(keyBase);
        keySpace.put(taskId, b.array());
        saveSnapShot();
        keyBase ++;
    }

    public synchronized void writeOutput(List<Message.DataPair> output, String taskId, List<Message.DataPair> metaDataPairs){
        if(!keySpace.containsKey(taskId)){
            generateKey(taskId);
        }
        WriteBatch batch = db.createWriteBatch();

        //METADATA
        batch.put(convertToKeySpace(SIZE_KEY, taskId, false), String.valueOf(output.size()).getBytes());
        for(Message.DataPair dataPair: metaDataPairs){
            batch.put(convertToKeySpace(dataPair.getKey(), taskId, false), dataPair.getValue().getBytes());
        }

        //DATA
        for (Message.DataPair dataPair: output) {
            batch.put(convertToKeySpace(dataPair.getKey(), taskId, true), dataPair.getValue().getBytes());
        }
        db.write(batch);
        try {
            batch.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized List<Message.DataPair> getOutput(String taskId, int minRange, int maxRange, Message.DataHandlerMessage.MetaData.Builder retrievedMetaData){
        if(!keySpace.containsKey(taskId)) return null;
        int cursor = 0;
        int keySpaceSize = 0;
        List<Message.DataPair> dataPairList = new LinkedList<Message.DataPair>();

        DBIterator iterator = db.iterator();
        iterator.seek(keySpace.get(taskId));
        //Process MetaData
        for(; iterator.hasNext(); iterator.next()){
            if(isData(iterator.peekNext().getKey())) break;

            if(retrievedMetaData != null){
                Map.Entry<byte[], byte[]> s = iterator.peekNext();
                String key = convertFromKeySpace(s.getKey(), taskId);
                if(key.equals(SIZE_KEY)){
                    keySpaceSize = Integer.parseInt(new String(s.getValue()));
                } else {
                    retrievedMetaData.addDataPair(Message.DataPair.newBuilder().setKey(key).setValue(new String(s.getValue())));
                }
            }
        }

        if(keySpaceSize == 0){
            return null;
        }

        iterator.prev();

        for(; iterator.hasNext(); iterator.next()) {
            //METADATA
            if (cursor == 0) {
                iterator.next();

                //MIN RANGE OFFSET
                for(int i = 0; i < minRange; i++){
                    iterator.next();
                    cursor ++;
                }
            }


            if(cursor >= keySpaceSize || cursor > maxRange)
                break;

            dataPairList.add(Message.DataPair.newBuilder().setValue(new String (iterator.peekNext().getValue())).setKey(new String(convertFromKeySpace(iterator.peekNext().getKey(), taskId))).build());
            cursor ++;
        }

        try {
            iterator.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dataPairList;
    }

    private byte [] convertToKeySpace(String key, String taskId, boolean data){
        byte dataType = (byte) (data ? 1:0);
        return ArrayUtils.addAll(ArrayUtils.add(keySpace.get(taskId), dataType), key.getBytes());
    }

    private boolean isData(byte[] key){
        return (key[keySpaceBits] == (byte) 1);
    }

    private String convertFromKeySpace(byte[] key, String taskId) {
        return new String(Arrays.copyOfRange(key, keySpace.get(taskId).length + 1, key.length));
    }
}