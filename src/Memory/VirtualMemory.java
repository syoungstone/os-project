package Memory;

import java.util.HashSet;
import java.util.Set;

public class VirtualMemory {

    private static VirtualMemory instance;

    private final Set<Page> pages;

    private VirtualMemory() {
        pages = new HashSet<>();
    }

    public static VirtualMemory getInstance() {
        if (instance == null) {
            instance = new VirtualMemory();
        }
        return instance;
    }

    public void storePage(Page page) {
        pages.add(page);
    }

    public void removePage(Page page) {
        pages.remove(page);
    }

}
