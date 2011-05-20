package torrefactor.core;

import torrefactor.util.Logger;
import torrefactor.util.ByteArrays;


public class UnknownMessage extends Message {
    private static final Logger LOG = new Logger();
    private static  byte id = -2;
    final byte[] data;

    public UnknownMessage (byte id) {
        this.id = id;
        this.data = null;
        LOG.warning("Unknown message with id " + id);
    }

    public UnknownMessage (byte id, byte[] data) {
        this.id = id;
        this.data = data;
        LOG.warning("Unknown message with id " + id
                    + " and lenght " + data.length + ": "
                    + ByteArrays.toHexString(data));
    }

    public byte id () {
        return UnknownMessage.id;
    }

    public byte[] toByteArray () {
        return ByteArrays.concat(new byte[][] {super.toByteArray(), this.data});
    }
}
