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

	/**
	 * Create a new SendSymmetricMessage for the given xor key.
	 *
	 * @param key	the xor key
	 */
    public SendSymmetricMessage (byte[] key) {
        this.key = key;
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
    public byte id () {
        return SendSymmetricMessage.id;
    }

	// Java does not override static method thus we cannot use @inheritDoc
	/**
	 * @{link torrefactor.core.Message#isValid(byte[]) Message}
	 */
    public static boolean isValid (byte[] msg) {
        return msg.length >= 1;
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
    public byte[] toByteArray () {
        return ByteArrays.concat(new byte[][] {super.toByteArray(), key});
    }

}
