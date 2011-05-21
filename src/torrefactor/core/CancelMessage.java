package torrefactor.core;

import torrefactor.core.DataBlockInfo;

/**
 * Represents a cancel message.
 */
public class CancelMessage extends RequestMessage {
    final static byte id = 8;

	/**
	 * Creates a new CancelMessage.
	 *
	 * @param info the DataBlockInfo representing the block to cancel.
	 */
    public CancelMessage (DataBlockInfo info) {
        super(info);
    }

	/**
	 * Creates a new CancelMessage from the given byte array representation.
	 *
	 * @param msg The byte array representation of the message.
	 */
    public CancelMessage (byte[] msg) {
        super(msg);
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
    public byte id () {
        return CancelMessage.id;
    }
}
