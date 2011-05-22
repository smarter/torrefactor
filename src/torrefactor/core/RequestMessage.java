package torrefactor.core;

import torrefactor.core.DataBlockInfo;
import torrefactor.util.ByteArrays;

/**
 * Represents a request message.
 *  id  1 byte
 *  index   4 byte
 *  begin   4 byte
 *  length  4 byte
 */
public class RequestMessage extends Message {
    final static byte id = 6;
    final int index;
    final int offset;
    final int length;

    /**
     * Create a new RequestMessage for a block.
     *
     * @param info the DataBlockInfo identifying the block
     */
    public RequestMessage (DataBlockInfo info) {
        index = info.pieceIndex();
        offset = info.offset();
        length = info.length();
    }

    /**
     * Create a new RequestMessage for the given byte array representation.
     *
     * @param msg    the byte array representation
     */
    public RequestMessage (byte[] msg) {
        index = ByteArrays.toInt(msg);
        offset = ByteArrays.toInt(msg, 4);
        length = ByteArrays.toInt(msg, 8);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte id () {
        return RequestMessage.id;
    }

    // Java does not override static method thus we cannot use @inheritDoc
    /**
     * {@link torrefactor.core.Message#isValid(byte[])}
     */
    public static boolean isValid (byte[] msg) {
        return msg.length == 12;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] toByteArray () {
        byte[] t = super.toByteArray();
        byte[] i = ByteArrays.fromInts(new int[] {index, offset, length});

        byte[] b = ByteArrays.concat(new byte[][] {t, i});
        System.err.println(ByteArrays.toHexString(b));

        return b;
    }
}
