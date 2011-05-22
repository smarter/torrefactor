package torrefactor.util;

import torrefactor.util.*;

import java.math.BigInteger;
import java.io.*;


/**
 * RsaInputStream wraps a stream and reads from it using Rsa.
 */
public class RsaInputStream extends InputStream {
    private static Logger LOG = new Logger();
    private Rsa rsa;
    private InputStream realStream;
    private DataInputStream dataRealStream;
    private int chunkLength; // in byte

    /**
     * Create a new RsaInputStream wrapping the stream with a chunk length
     * matching the length of the modulo (including sign bit).
     * If java didn't loved signed things, it would probably be handier to
     * exclude the sign bit.
     */
    public RsaInputStream (InputStream stream, Rsa rsa) {
        this(stream, rsa, rsa.getModulo().length);
    }

    /**
     * Create a new RsaInputStream wrapping the stream using the given chunk
     * length if possible.
     */
    public RsaInputStream (InputStream stream, Rsa rsa, int chunkLength) {
        this.realStream = stream;
        this.dataRealStream = new DataInputStream(stream);
        this.rsa = rsa;

        // chunkLength must be at least the length of the modulo
        int minLen = this.rsa.getModulo().length;
        if (chunkLength < minLen) {
            LOG.warning("chunkLength " + chunkLength + " is smaller than the "
                        + "minimum length of " + minLen);
            chunkLength = minLen;
        }

        this.chunkLength = chunkLength;
        LOG.info("Initialized RsaInputStream with chunkLength "
                       + this.chunkLength);
    }

    /**
     * Closes this output stream and releases any system resources associated
     * with this stream.
     */
    public void close () throws IOException {
        this.realStream.close();
        super.close();
    }

    /**
     * Reads the next byte of data from the input stream.
     */
    public int read () throws IOException {
        byte[] array = new byte[this.chunkLength];
        this.dataRealStream.readFully(array);
        
        BigInteger bi = new BigInteger(array);
        bi = this.rsa.decrypt(bi);

        int b = bi.intValue();
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
     * Returns the chunkLength used by this stream.
     */
    public int getChunkLength () {
        return this.chunkLength;
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
