package torrefactor.core;

import torrefactor.util.ByteArrays;

/**
 * Represents a SendRsa message.
 *  id          1 byte
 *  chunkLength 4 byte
 *  keyLength   4 byte
 *  key         x byte
 *  modLength   4 byte
 *  modulo      y byte
 */
public class SendRsaMessage extends Message {
    final static byte id = 10;
    final byte [] key;
    final byte [] modulo;
    final int chunkLength;

    public SendRsaMessage (byte[] key, byte[] modulo, int chunkLength) {
        this.key = key;
        this.modulo = modulo;
        this.chunkLength = chunkLength;
    }

    public SendRsaMessage (byte[] msg) {
        this.chunkLength = (ByteArrays.toInt(msg)/8) + 1;
        int keyLen = ByteArrays.toInt(msg, 4);
        this.key = new byte[keyLen];
        System.arraycopy(msg, 8, this.key, 0, keyLen);
        int modLen = ByteArrays.toInt(msg, 8 + keyLen);
        this.modulo = new byte[modLen];
        System.arraycopy(msg, 12 + keyLen, this.modulo, 0, modLen);
    }

    public byte id () {
        return SendRsaMessage.id;
    }

    public static boolean isValid (byte[] msg) {
        return msg.length >= 15;
    }

    public byte[] toByteArray () {
        byte[] keyLength = ByteArrays.fromInt(key.length);
        byte[] moduloLength = ByteArrays.fromInt(modulo.length);
        byte[] chunkLen = ByteArrays.fromInt((chunkLength-1)*8);

        return ByteArrays.concat(new byte[][] 
                {super.toByteArray(), chunkLen, keyLength, key,
                 moduloLength, modulo});
    }

}
