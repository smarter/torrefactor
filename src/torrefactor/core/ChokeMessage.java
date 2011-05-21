package torrefactor.core;

/**
 * Represents a choke message.
 */
public class ChokeMessage extends Message {
    final static byte id = 0;

	/**
	 * {@inheritDoc}
	 */
	@Override
    public byte id () {
        return ChokeMessage.id;
    }
}
