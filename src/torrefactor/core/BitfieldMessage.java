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

    /**
     * Create a new BitFieldMessage for the given bitfield.
     */
    public BitfieldMessage (byte[] bitfield) {
        this.bitfield = bitfield;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte id () {
        return BitfieldMessage.id;
    }

    // Java does not override static method thus we cannot use @inheritDoc
    /**
     * {@link torrefactor.core.Message#isValid(byte[])}
     */
    public static boolean isValid (byte[] msg) {
        return msg.length >= 2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] toByteArray () {
        byte[] t = super.toByteArray();

        return ByteArrays.concat(new byte[][] {t, bitfield});
    }
}
