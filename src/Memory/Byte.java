package Memory;

// A class used to represent a byte of information
public class Byte {
    // Since this is just simulated memory, we store the physical address
    // from which this imaginary byte would have been retrieved
    private final int byteAddress;
    Byte(int byteAddress) {
        this.byteAddress = byteAddress;
    }
}
