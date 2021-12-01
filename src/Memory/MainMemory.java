package Memory;

import java.util.*;

public class MainMemory {

    private static MainMemory instance;

    private static final int CAPACITY_IN_MB = 1024;
    private static final int PAGE_SIZE_IN_MB = 2;
    private static final int MAX_PHYSICAL_ADDRESS = 1024 * 1024 * CAPACITY_IN_MB - 1;

    private final List<Frame> frames;
    private final Queue<Integer> usedFrames;
    private final Queue<Integer> freeFrameList;

    private MainMemory() {
        int numFrames = CAPACITY_IN_MB / PAGE_SIZE_IN_MB;
        frames = new ArrayList<>();
        usedFrames = new LinkedList<>();
        freeFrameList = new LinkedList<>();
        for (int i = 0 ; i < numFrames ; i++) {
            int startAddress = i * Page.getSizeBytes();
            frames.add(new Frame(startAddress));
            freeFrameList.add(i);
        }
    }

    public static MainMemory getInstance() {
        if (instance == null) {
            instance = new MainMemory();
        }
        return instance;
    }

    public synchronized List<Page> requestMemory(int requestSizeMB) {

        int requestOverPageSize = requestSizeMB / PAGE_SIZE_IN_MB;
        int remainder = requestSizeMB % PAGE_SIZE_IN_MB;
        int pagesRequired = remainder > 0 ? requestOverPageSize + 1 : requestOverPageSize;
        int framesAvailable = freeFrameList.size();
        List<Page> pages = new ArrayList<>();

        if (framesAvailable > 0) {
            for (int i = 0 ; i < framesAvailable ; i++) {
                int frameNumber = freeFrameList.remove();
                int startAddress = i * Page.getSizeBytes();
                Page page = new Page(startAddress, frameNumber);
                Frame frame = frames.get(frameNumber);
                frame.setPage(page);
                pages.add(page);
                usedFrames.add(frameNumber);
            }
        }

        if (pagesRequired - framesAvailable > 0) {
            for (int i = 0 ; i < pagesRequired - framesAvailable ; i++) {
                int startAddress = i * Page.getSizeBytes();
                pages.add(new Page(startAddress));
            }
        }

        return pages;
    }

    public synchronized void releaseMemory(List<Page> pages) {
        for (Page page : pages) {
            if (page.isInMemory()) {
                int frameNumber = page.getFrameNumber();
                Frame frame = frames.get(frameNumber);
                frame.removePage();
                usedFrames.remove(frameNumber);
                freeFrameList.add(frameNumber);
            } else {
                VirtualMemory.getInstance().removePage(page);
            }
        }
    }

    public synchronized Word read(Page page, int offset) {
        requirePage(page);
        int frameNumber = page.getFrameNumber();
        Frame frame = frames.get(frameNumber);
        int physicalAddress = frame.getStartAddress() + offset;
        return new Word(physicalAddress);
    }

    // Since each memory address references a byte, and we want to retrieve a word
    // consisting of multiple bytes, we need to handle the case where we want a word
    // that straddles two pages
    public synchronized Word readAcrossPageBreak(Page page1, int offset, Page page2) {
        requirePage(page1);
        requirePage(page2);
        int frameNumber1 = page1.getFrameNumber();
        int frameNumber2 = page2.getFrameNumber();
        Frame frame1 = frames.get(frameNumber1);
        Frame frame2 = frames.get(frameNumber2);
        int physicalAddress1 = frame1.getStartAddress() + offset;
        int sizeFirstChunk = Page.getSizeBytes() - offset;
        int physicalAddress2 = frame2.getStartAddress();
        return new Word(physicalAddress1, sizeFirstChunk, physicalAddress2);
    }

    private void requirePage(Page page) {
        if (!page.isInMemory()) {
            // Page fault
            // First check if free frame in memory
            if (freeFrameList.isEmpty()) {
                // Swap out victim page
                Page victim = swapOut();
                victim.swapOut();
                VirtualMemory.getInstance().storePage(victim);
            }
            // Swap in requested page
            swapIn(page);
            VirtualMemory.getInstance().removePage(page);
        }
    }

    private void swapIn(Page page) {
        int frameNumber = freeFrameList.remove();
        Frame frame = frames.get(frameNumber);
        page.setFrameNumber(frameNumber);
        frame.setPage(page);
    }

    // Uses FIFO algorithm to select victim page
    private Page swapOut() {
        int freedFrameNumber = usedFrames.remove();
        Frame freedFrame = frames.get(freedFrameNumber);
        Page victim = freedFrame.removePage();
        freeFrameList.add(freedFrameNumber);
        return victim;
    }

}
