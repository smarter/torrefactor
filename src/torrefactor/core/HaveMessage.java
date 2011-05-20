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

    public HaveMessage (int index) {
        this.index = index;
    }

    public HaveMessage (byte[] msg) {
        this.index = ByteArrays.toInt(msg);
    }

    public byte id () {
        return HaveMessage.id;
    }

    public static boolean isValid (byte[] msg) {
        return msg.length == 5;
    }

    public byte[] toByteArray () {
        byte[] t = super.toByteArray();
        byte[] i = ByteArrays.fromInt(index);

        return ByteArrays.concat(new byte[][] {t, i});
    }
}
