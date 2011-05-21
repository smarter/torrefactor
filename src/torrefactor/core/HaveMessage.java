package torrefactor.core;

import torrefactor.util.ByteArrays;

/**
 * Represents a have message.
 *  id          1 byte
 *  pieceIndex  4 byte
 */
public class HaveMessage extends Message {
    final static byte id = 4;
    final int index;

    /**
     * Create a new HaveMessage for the given index.
     *
     * @param index the piece index
     */
    public HaveMessage (int index) {
        this.index = index;
    }

    /**
     * Create a new HaveMessage for the given byte array representation.
     *
     * @param msg the byte array representation from which to build the
     * message.
     */
    public HaveMessage (byte[] msg) {
        this.index = ByteArrays.toInt(msg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte id () {
        return HaveMessage.id;
    }

    // Java does not override static method thus we cannot use @inheritDoc
    /**
     * @{link torrefactor.core.Message#isValid(byte[]) Message}
     */
    public static boolean isValid (byte[] msg) {
        return msg.length == 5;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] toByteArray () {
        byte[] t = super.toByteArray();
        byte[] i = ByteArrays.fromInt(index);

        return ByteArrays.concat(new byte[][] {t, i});
    }
}
