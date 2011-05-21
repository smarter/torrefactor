package torrefactor.core;

import torrefactor.util.Logger;
import torrefactor.util.ByteArrays;


/**
 * Represents an unknown message. This message does not have a fixed id. The id
 * is set when constructing the message so it could reflect what has been
 * received on the wire.
 */
public class UnknownMessage extends Message {
    private static final Logger LOG = new Logger();
    private static  byte id = -2;
    final byte[] data;

	/**
	 * Construct a new UnknownMessage with the given id and no data.
	 *
	 * @param	id		id of the message
	 */
    public UnknownMessage (byte id) {
        this.id = id;
        this.data = null;
        LOG.warning("Unknown message with id " + id);
    }

	/**
	 * Construct a new UnknownMessage with the given id and the given data.
	 *
	 * @param	id		id of the message
	 * @param	data	the data contained in this message
	 */
    public UnknownMessage (byte id, byte[] data) {
        this.id = id;
        this.data = data;
        LOG.warning("Unknown message with id " + id
                    + " and lenght " + data.length + ": "
                    + ByteArrays.toHexString(data));
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
    public byte id () {
        return UnknownMessage.id;
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
    public byte[] toByteArray () {
        return ByteArrays.concat(new byte[][] {super.toByteArray(), this.data});
    }
}
