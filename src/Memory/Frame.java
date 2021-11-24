package Memory;

public class Frame {

    static final int SIZE_IN_MB = 2;
    private final int startAddress;

    private Page page;

    Frame(int startAddress) {
        this.startAddress = startAddress;
    }

    int getStartAddress() {
        return startAddress;
    }

    public Page removePage() {
        Page victim = page;
        page = null;
        return victim;
    }

    public void setPage(Page page) {
        this.page = page;
    }
}
