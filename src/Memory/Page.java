package Memory;

public class Page {

    private final int startAddress;
    private Integer frameNumber;
    private boolean inMemory;

    Page(int startAddress) {
        this.startAddress = startAddress;
        this.frameNumber = null;
        this.inMemory = false;
    }

    Page(int startAddress, int frameNumber) {
        this.startAddress = startAddress;
        this.frameNumber = frameNumber;
        this.inMemory = true;
    }

    public static int getSizeBytes() {
        return 1024 * 1024 * Frame.SIZE_IN_MB;
    }

    public int getStartAddress() {
        return startAddress;
    }

    public int getFrameNumber() {
        return frameNumber;
    }

    public boolean isInMemory() {
        return inMemory;
    }

    public void setFrameNumber(int frameNumber) {
        this.frameNumber = frameNumber;
        this.inMemory = true;
    }

    public void swapOut() {
        this.frameNumber = null;
        this.inMemory = false;
    }

}
