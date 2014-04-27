package Core;

/**
 * Created by linmichaelj on 2/1/14.
 */
public class Status {
    public enum State {LOADING, READY, RUNNING, INTERRUPTED, RECOVERY, FINISHED};

    private String id;
    private State state;

    public Status(String id){
        this.id = id;
        this.state = State.LOADING;
    }

    public String getId() {
        return id;
    }

    public State getState() {
        return state;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String toString(){
        return "Id : " + id + ", State: " + state;
    }
}
