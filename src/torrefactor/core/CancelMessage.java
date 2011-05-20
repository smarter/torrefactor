package torrefactor.core;

import torrefactor.core.DataBlockInfo;

/**
 * Represents a cancel message.
 */
public class CancelMessage extends RequestMessage {
    final static byte id = 8;

    public CancelMessage (DataBlockInfo info) {
        super(info);
    }

    public CancelMessage (byte[] msg) {
        super(msg);
    }

    public byte id () {
        return CancelMessage.id;
    }
}
