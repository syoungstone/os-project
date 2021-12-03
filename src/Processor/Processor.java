package Processor;

import Processes.PCB;

public class Processor {

    private final Core core1;
    private final Core core2;

    public Processor() {
        core1 = new Core(new SJFScheduler());
        core2 = new Core(new MLQScheduler());
    }

    public void request(PCB p) {
        // Process always sent to the same core
        // This allows us to more easily measure core performance
        CoreId coreId = p.getCoreId();
        if (coreId == CoreId.CORE1) {
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

    public void registerTermination(PCB p) {
        CoreId coreId = p.getCoreId();
        if (coreId == CoreId.CORE1) {
            core1.registerTermination(p);
        } else {
            core2.registerTermination(p);
        }
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

    public void printStatistics() {
        System.out.println("\nComparison of core performance:");
        System.out.println("\n\tCore 1: " + core1.getSchedulerName());
        core1.printStatistics();
        System.out.println("\n\tCore 2: " + core2.getSchedulerName());
        core2.printStatistics();
    }


    public enum CoreId {
        CORE1, CORE2
    }
}