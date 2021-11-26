package Processor;

import Processes.PCB;

public class Processor {

    private final Core core1;
    private final Core core2;

    public Processor() {
        core1 = new Core(new SJFScheduler());
        core2 = new Core(new RRScheduler());
    }

    public void request(PCB p) {
        // Randomly assign to one of the cores
        if (Math.random() < 0.5) {
            core1.request(p);
        } else {
            core2.request(p);
        }
    }

    public void start() {
        core1.start();
        core2.start();
    }

    public void advance() {
        core1.advance();
        core2.advance();
    }

    public void stop() {
        core1.stop();
        core2.stop();
    }

    public boolean cycleFinished() {
        return core1.cycleFinished() && core2.cycleFinished();
    }

    public String getCurrentPids() {
        return "\n\tCore 1: " + core1.getCurrentPids() + "\n\tCore 2: " + core2.getCurrentPids();
    }

    public int getReadyCount() {
        return core1.getReadyCount() + core2.getReadyCount();
    }

}