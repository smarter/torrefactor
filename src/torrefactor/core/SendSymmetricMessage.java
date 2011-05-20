package torrefactor.core;

import torrefactor.util.ByteArrays;

/**
 * Represents a SendSymmetric message.
 *  id  1 byte
 *  key x byte
 */
public class SendSymmetricMessage extends Message {
    final static byte id = 11;
    final byte [] key;

    public SendSymmetricMessage (byte[] key) {
        this.key = key;
    }

    public byte id () {
        return SendSymmetricMessage.id;
    }

    public static boolean isValid (byte[] msg) {
        return msg.length >= 1;
    }

    public byte[] toByteArray () {
        return ByteArrays.concat(new byte[][] {super.toByteArray(), key});
    }

}
