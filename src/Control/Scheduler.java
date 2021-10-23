package Control;

public abstract class Scheduler {
    public abstract void add(int pid);
    public abstract int remove();
}
