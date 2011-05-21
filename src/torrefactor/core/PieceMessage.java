package torrefactor.core;

import torrefactor.util.ByteArrays;


/**
 * Represents a piece message.
 *  id      1 byte
 *  index   4 byte
 *  offset  4 byte
 *  block   x byte
 */
public class PieceMessage extends Message {
    final static byte id = 7;
    final int index;
    final int offset;
    final byte[] block;

    /**
     * Create a new PieceMessage.
     *
     * @param index        the index of the piece
     * @param offset    the offset within the piece
     * @param block        the data block
     */
    public PieceMessage (int index, int offset, byte[] block) {
        this.index = index;
        this.offset = offset;
        this.block = block;
    }

    /**
     * Create a new PieceMessage from the given byte array representation.
     *
     * @param msg    the byte array representation from which to build this
     *                message.
     */
    public PieceMessage (byte[] msg) {
        this.index = ByteArrays.toInt(msg);
        this.offset = ByteArrays.toInt(msg, 4);
        int len = msg.length - 8;
        this.block = new byte[len];
        System.arraycopy(msg, 8, this.block, 0, len);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte id () {
        return PieceMessage.id;
    }

    // Java does not override static method thus we cannot use @inheritDoc
    /**
     * @{link torrefactor.core.Message#isValid(byte[]) Message}
     */
    public static boolean isValid (byte[] msg) {
        return msg.length >= 9;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] toByteArray () {
        byte[] t = super.toByteArray();
        byte[] i = ByteArrays.fromInts(new int[] {index, offset});
        
        return ByteArrays.concat(new byte[][] {t, i});
    }
}
