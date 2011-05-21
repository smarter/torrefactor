package torrefactor.core;

/**
 * Represents a keep-alive message.
 */
public class KeepAliveMessage extends Message {
    final static byte id = 1;

	/**
	 * {@inheritDoc}
	 */
	@Override
    public byte id () {
        return KeepAliveMessage.id;
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
    public byte[] toByteArray () {
        return new byte[] {0, 0, 0, 0};
    }
}
