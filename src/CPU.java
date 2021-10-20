import java.util.List;

public class CPU {

    private static CPU instance;

    private Process p;

    private CPU() {}

    public static CPU getInstance() {
        if (instance == null) {
            instance = new CPU();
        }
        return instance;
    }

    public Process getProcess() {
        return p;
    }

    public void setProcess(Process p) {
        this.p = p;
    }

}
