package Processor;

import Processes.PCB;

// Used by the Core class to measure scheduler performance
public class StatisticalUnit {

    private boolean running = false;

    private long totalRunTime = 0;

    private long startTime = 0;

    private int completedProcesses = 0;

    private long totalTurnaroundTime = 0;
    private long totalWaitingTime = 0;

    private int utilizedCycles = 0;
    private int totalCycles = 0;

    public void start() {
        running = true;
        startTime = System.currentTimeMillis();
    }

    public void stop() {
        running = false;
        totalRunTime += (System.currentTimeMillis() - startTime);
    }

    public synchronized void incrementUtilizedCycles() {
        utilizedCycles++;
    }

    public synchronized void incrementTotalCycles() {
        totalCycles++;
    }

    public synchronized void registerTermination(PCB p) {
        completedProcesses++;
        totalTurnaroundTime += p.getTurnaroundTime();
        totalWaitingTime += p.getWaitingTime();
    }

    public synchronized double getUtilization() {
        // Prevent division by 0
        if (totalCycles == 0) {
            return 0;
        }
        return (double) utilizedCycles / totalCycles;
    }

    public synchronized double getThroughput() {
        double secondsElapsed;
        if (running) {
            secondsElapsed = (double) (totalRunTime + System.currentTimeMillis() - startTime) / 1000;
        } else {
            secondsElapsed = (double) totalRunTime / 1000;
        }
        // Prevent division by 0
        if (secondsElapsed == 0) {
            return 0;
        }
        return (double) completedProcesses / secondsElapsed;
    }

    public synchronized double getAvgTurnaroundTime() {
        // Prevent division by 0
        if (completedProcesses == 0) {
            return 0;
        }
        return (double) totalTurnaroundTime / completedProcesses;
    }

    public synchronized double getAvgWaitingTime() {
        // Prevent division by 0
        if (completedProcesses == 0) {
            return 0;
        }
        return (double) totalWaitingTime / completedProcesses;
    }
}
