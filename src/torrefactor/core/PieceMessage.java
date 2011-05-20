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

    public PieceMessage (int index, int offset, byte[] block) {
        this.index = index;
        this.offset = offset;
        this.block = block;
    }

    public PieceMessage (byte[] msg) {
        this.index = ByteArrays.toInt(msg);
        this.offset = ByteArrays.toInt(msg, 4);
        int len = msg.length - 8;
        this.block = new byte[len];
        System.arraycopy(msg, 8, this.block, 0, len);
    }

    public byte id () {
        return PieceMessage.id;
    }

    public static boolean isValid (byte[] msg) {
        return msg.length >= 9;
    }

    public byte[] toByteArray () {
        byte[] t = super.toByteArray();
        byte[] i = ByteArrays.fromInts(new int[] {index, offset});
        
        return ByteArrays.concat(new byte[][] {t, i});
    }
}
