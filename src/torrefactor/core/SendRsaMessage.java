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

	/**
	 * Create a new SendRsaMessage.
	 *
	 * @param key			Rsa key
	 * @param modulo		Modulo to use with the key
	 * @param chunkLength	The chunk length to use when using this key
	 */
    public SendRsaMessage (byte[] key, byte[] modulo, int chunkLength) {
        this.key = key;
        this.modulo = modulo;
        this.chunkLength = chunkLength;
    }

	/**
	 * Create a new SendRsaMessage from the given byte array representation.
	 *
	 * @param msg	The byte array representation
	 */
    public SendRsaMessage (byte[] msg) {
        this.chunkLength = (ByteArrays.toInt(msg)/8) + 1;
        int keyLen = ByteArrays.toInt(msg, 4);
        this.key = new byte[keyLen];
        System.arraycopy(msg, 8, this.key, 0, keyLen);
        int modLen = ByteArrays.toInt(msg, 8 + keyLen);
        this.modulo = new byte[modLen];
        System.arraycopy(msg, 12 + keyLen, this.modulo, 0, modLen);
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
    public byte id () {
        return SendRsaMessage.id;
    }

	// Java does not override static method thus we cannot use @inheritDoc
	/**
	 * @{link torrefactor.core.Message#isValid(byte[]) Message}
	 */
    public static boolean isValid (byte[] msg) {
        return msg.length >= 15;
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
    public byte[] toByteArray () {
        byte[] keyLength = ByteArrays.fromInt(key.length);
        byte[] moduloLength = ByteArrays.fromInt(modulo.length);
        byte[] chunkLen = ByteArrays.fromInt((chunkLength-1)*8);

        return ByteArrays.concat(new byte[][] 
                {super.toByteArray(), chunkLen, keyLength, key,
                 moduloLength, modulo});
    }

}
