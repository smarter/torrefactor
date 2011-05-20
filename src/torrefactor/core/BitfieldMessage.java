package torrefactor.core;

import torrefactor.util.ByteArrays;

/**
 * Represents a bitfield message.
 *  id          1 byte
 *  bitfield    x byte
 */
public class BitfieldMessage extends Message {
    final static byte id = 5;
    final byte[] bitfield;

    public BitfieldMessage (byte[] bitfield) {
        this.bitfield = bitfield;
    }

    public byte id () {
        return BitfieldMessage.id;
    }

    public static boolean isValid (byte[] msg) {
        return msg.length >= 2;
    }

    public byte[] toByteArray () {
        byte[] t = super.toByteArray();

        return ByteArrays.concat(new byte[][] {t, bitfield});
    }
}
