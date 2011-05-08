package torrefactor.util;

import torrefactor.util.*;

import java.io.*;
import java.math.BigInteger;


/**
 * RsaOutputStream wraps a stream and writes to it using RSA.
 */
public class RsaOutputStream extends OutputStream {
    private static Logger LOG = new Logger();
    OutputStream realStream;
    Rsa rsa;
    int chunkLength; // length of chunk in bytes

    /**
     * Create a new RsaOutputStream wrapping the stream.
     */
    public RsaOutputStream (OutputStream stream, Rsa rsa) {
        this(stream, rsa, 0);
    }

    /**
     * Create a new RsaOutputStream wrapping the stream using the given chunk
     * length if possible.
     */
    public RsaOutputStream (OutputStream stream, Rsa rsa, int chunkLength) {
        this.realStream = stream;
        this.rsa = rsa;

        int minLen = this.rsa.getModulo().length;
        if (chunkLength < minLen) {
            chunkLength = minLen;
        }
        this.chunkLength = chunkLength;

        LOG.info(this, "Initialized RsaOutputStream.");
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
     * Flushes this output stream and forces any buffered output
     * bytes to be written out.
     * */
    public void flush () throws IOException {
        this.realStream.flush();
    }

    /**
     * Write a byte to this output stream.
     */
    public void write (int b) throws IOException {
        // Ignore the 24 higher bytes of the int since it is a byte
        // (sometimes there's random values in those bytes)
        b &= 0xFF;

        BigInteger bi = BigInteger.valueOf(b);
        bi = this.rsa.encrypt(bi);
        byte[] array = bi.toByteArray();

        byte[] realArray = ByteArrays.fromBigInteger(bi, this.chunkLength);

        this.realStream.write(realArray);
    }

    /**
     * Returns the OutputStream wrapped in this stream. You can use this
     * function to unwrap the OutputStream.
     */
    public OutputStream getWrappedStream () {
        return this.realStream;
    }

    /**
     * Returns the chunkLength used by this stream.
     */
    public int getChunkLength () {
        return this.chunkLength;
    }
}