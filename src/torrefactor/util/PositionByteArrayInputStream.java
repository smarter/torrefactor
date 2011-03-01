package torrefactor.util;

import java.io.ByteArrayInputStream;

public class PositionByteArrayInputStream extends ByteArrayInputStream {
    public PositionByteArrayInputStream(byte[] array) {
        super(array);
    }

    public int position() {
        return super.pos;
    }
}
