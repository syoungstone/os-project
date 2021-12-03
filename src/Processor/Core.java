package Processor;

import Control.OperatingSystem;
import Processes.PCB;
import Processes.State;

public class Core {

    private static final int NUM_HARDWARE_THREADS = 4;

    private final Object threadCoordinator;
    private final ShortTermScheduler shortTermScheduler;
    private final HardwareThread[] hardwareThreads;

    Core(ShortTermScheduler scheduler) {
        threadCoordinator = new Object();
        shortTermScheduler = scheduler;
        hardwareThreads = new HardwareThread[NUM_HARDWARE_THREADS];
        for (int i = 0; i < NUM_HARDWARE_THREADS; i++) {
            hardwareThreads[i] = new HardwareThread();
        }
    }

    public void start() {
        for (HardwareThread hardwareThread : hardwareThreads) {
            hardwareThread.start();
        }
    }

    public void advance() {
        // Notify waiting CPU threads to start a new cycle
        synchronized (threadCoordinator) {
            threadCoordinator.notifyAll();
        }
    }

    public void stop() {
        for (HardwareThread hardwareThread : hardwareThreads) {
            hardwareThread.stop();
        }
    }

    public boolean cycleFinished() {
        boolean finished = true;
        for (HardwareThread hardwareThread : hardwareThreads) {
            finished = finished && hardwareThread.isWaiting();
        }
        return finished;
    }

    public void request(PCB p) {
        shortTermScheduler.add(p);
    }

    public int getReadyCount() {
        return shortTermScheduler.getReadyCount();
    }

    public String getCurrentPids() {
        StringBuilder pids = new StringBuilder();
        for (int i = 0; i < hardwareThreads.length ; i++) {
            pids.append(hardwareThreads[i].getCurrentPid());
            if (i < hardwareThreads.length - 1) {
                pids.append(", ");
            }
        }
        return pids.toString();
    }

    private class HardwareThread {

        private PCB p;
        private int counter;
        private Thread thread;

        HardwareThread() {
            counter = 0;
            instantiateThread();
        }

        void start() {
            thread.start();
        }

        void stop() {
            thread.interrupt();
            instantiateThread();
        }

        boolean isWaiting() {
            return thread.getState() == Thread.State.WAITING;
        }

        int getCurrentPid() {
            if (p == null) {
                return OperatingSystem.KERNEL_ID;
            } else {
                return p.getPid();
            }
        }

        private void instantiateThread() {
            thread = new Thread(() -> {
                boolean run = true;
                while (run) {
                    try {
                        synchronized (threadCoordinator) {
                            threadCoordinator.wait();
                        }
                        advance();
                    } catch (InterruptedException e) {
                        run = false;
                    }
                }
            });
        }

        private void scheduleNew() {
            counter = 0;
            if (p != null && p.getState() == State.RUN) {
                p.setState(State.READY);
                shortTermScheduler.add(p);
            }
            p = shortTermScheduler.remove();
            if (p != null) {
                p.setState(State.RUN);
            }
        }

        private void advance() {
            if ((p == null && getReadyCount() > 0) ||
                    (p != null && p.getState() != State.RUN)){
                scheduleNew();
            }
            if (p != null) {
                p.progressOneCycle();
                counter++;
                if (shortTermScheduler.scheduleNew(counter) || p.getState() != State.RUN) {
                    scheduleNew();
                }
            }
        }

    }
    
}