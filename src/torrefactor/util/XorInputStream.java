package torrefactor.util;

import torrefactor.util.*;

import java.io.*;


/**
 * XorInputStream wraps a stream and reads from it using Rsa.
 */
public class XorInputStream extends InputStream {
    private static Logger LOG = new Logger();
    private InputStream realStream;
    private byte[] key;
    private int keyOffset = 0;
    private int keyLength;

    /**
     * Create a new XorInputStream wrapping the stream.
     */
    public XorInputStream (InputStream stream, byte[] key, int len) {
        this.realStream = stream;
        this.key = key;
        this.keyLength = len;
        LOG.info(this, "Initialized XorInputStream with key length "
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
     * Reads the next byte of data from the input stream.
     */
    public int read () throws IOException {
        int b = this.realStream.read();
        b ^= ((int) this.key[this.keyOffset] & 0xFF);
        
        this.keyOffset += 1;
        if (this.keyOffset >= this.keyLength) {
            this.keyOffset = 0;
        }

        return b;
    }

    /**
     * Returns the InputStream wrapped in this stream. You can use this
     * function to unwrap the InputStream.
     */
    public InputStream getWrappedStream () {
        return this.realStream;
    }

    /**
     * Returns something different than 0 if some data are waiting on the
     * socket.
     *
     * @see java.io.InputStream#available()
     */
    public int available () throws IOException {
        return this.realStream.available();
    }
}
