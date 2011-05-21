package torrefactor.core;

import torrefactor.util.ByteArrays;

/**
 * This is the parent class of all messages. The variable id MUST be overriden
 * by classes extending this class.
 */
public abstract class Message {

    // Should be abstract but java doesn't allow abstract variables. Thus we
    // default to something stupid so we may have a better clue if it goes
    // wrong.
    //final static byte id = -42;

    /**
	 * Workaround to emulate overiding class variable: this methods must return
     * the class variable id.
	 */
    abstract byte id();
    
    /**
	 * Check if the given byte array can be interpreted as a valid Message.
	 *
	 * @param	msg		the byte array on which the check is made
	 * @return	true if msg can be interpreted as a valid message for this
	 *			class
	 */
    public static boolean isValid (byte[] msg) {
        return msg.length == 0;
    }

    /**
     * Returns the byte array representation of this message as it should be
     * sent on the wire.
	 *
	 * @return the byte array representation of this message
     */
    public byte[] toByteArray () {
        return new byte[] {id()};
    }
}
