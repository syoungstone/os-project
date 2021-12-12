package Memory;

// A class used to represent a word of information
public class Word {

    public static final int WORD_SIZE_IN_BYTES = 4;

    private Byte[] bytes;

    // Reading a word from a single location
    Word(int physicalAddress) {
        bytes = new Byte[WORD_SIZE_IN_BYTES];
        for (int i = 0 ; i < WORD_SIZE_IN_BYTES ; i++) {
            int byteAddress = physicalAddress + i;
            bytes[i] = new Byte(byteAddress);
        }
    }

    // Reading a word with bytes in two locations because of a page break
    Word(int physicalAddress1, int sizeFirstChunk, int physicalAddress2) {
        bytes = new Byte[WORD_SIZE_IN_BYTES];
        for (int i = 0 ; i < sizeFirstChunk ; i++) {
            int byteAddress = physicalAddress1 + i;
            bytes[i] = new Byte(byteAddress);
        }
        for (int i = 0 ; i < WORD_SIZE_IN_BYTES - sizeFirstChunk ; i++) {
            int byteAddress = physicalAddress2 + i;
            bytes[sizeFirstChunk + i] = new Byte(byteAddress);
        }
    }

}
