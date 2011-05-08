package torrefactor.util;

import torrefactor.util.*;

import java.io.*;

/**
 * XorOutputStream wraps a stream and writes to it using RSA.
 */
public class XorOutputStream extends OutputStream {
    private static Logger LOG = new Logger();
    OutputStream realStream;
    private byte[] key;
    private int keyOffset = 0;
    private int keyLength;

    /**
     * Create a new XorOutputStream wrapping the stream.
     */
    public XorOutputStream (OutputStream stream, byte[] key, int len) {
        this.realStream = stream;
        this.key = key;
        this.keyLength = len;
        LOG.info(this, "Initialized XorOutputStream with key length "
                       + len);
    }

    /**
     * Closes this output stream and releases any system resources associated
     * with this stream.
     */
    public void close () throws IOException {
        this.realStream.close();
    }

    /**
     * Flushes this output stream and forces any buffered output
     * bytes to be written out.
     */
    public void flush () throws IOException {
        this.realStream.flush();
    }

    /**
     * Write a byte to this output stream.
     */
    public void write (int b) throws IOException {
        b ^= this.key[this.keyOffset];

        this.keyOffset += 1;
        if (this.keyOffset >= this.keyLength) {
            this.keyOffset = 0;
        }

        this.realStream.write(b);
    }

    /**
     * Returns the OutputStream wrapped in this stream. You can use this
     * function to unwrap the OutputStream.
     */
    public OutputStream getWrappedStream () {
        return this.realStream;
    }
}
