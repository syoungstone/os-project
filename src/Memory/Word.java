package Memory;

public class Word {

    public static final int WORD_SIZE_IN_BYTES = 4;

    private Byte[] bytes;

    Word(int physicalAddress) {
        bytes = new Byte[WORD_SIZE_IN_BYTES];
    }

}
