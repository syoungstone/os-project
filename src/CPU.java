public class CPU {

    private static CPU instance;

    private PCB p;

    private CPU() {}

    public static CPU getInstance() {
        if (instance == null) {
            instance = new CPU();
        }
        return instance;
    }

    public void setProcess(PCB p) {
        this.p = p;
    }

}
