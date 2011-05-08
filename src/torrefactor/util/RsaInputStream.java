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
     * Create a new RsaInputStream wrapping the stream.
     */
    public RsaInputStream (InputStream stream, Rsa rsa) {
        this.realStream = stream;
        this.dataRealStream = new DataInputStream(stream);
        this.rsa = rsa;

        int len = this.rsa.getModulo().length;
        if ((len-1) % 8 != 0) {
            LOG.error("bitlength of modulo " + len
                            + " is not equal to (k*8)+1.");
        }
        this.chunkLength = len;
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
        LOG.debug("readFully... ");
        this.dataRealStream.readFully(array);
        LOG.debug("done.");
        
        BigInteger bi = new BigInteger(array);
        bi = this.rsa.decrypt(bi);

        int b = bi.intValue();
        System.out.print(".");
        return b;
    }

    /**
     * Returns the InputStream wrapped in this stream. You can use this
     * function to unwrap the InputStream.
     */
    public InputStream getWrappedStream () {
        return this.realStream;
    }
}
