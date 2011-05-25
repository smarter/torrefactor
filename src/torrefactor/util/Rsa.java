package torrefactor.util;

import java.util.Random;
import java.security.SecureRandom;
import java.math.BigInteger;

/**
 * This class generate a key pair, encrypt and decryt using RSA
 */
public class Rsa {
    private static Logger LOG = new Logger();
    private BigInteger privateKey = null;
    private BigInteger publicKey = null;
    private BigInteger encryptKey = null;
    private BigInteger encryptModulo = null;
    private BigInteger modulo;
    private int bitLength;

    /**
     * Create a Rsa object using the specified keys, modulo and bitlength.
     */
    public Rsa (BigInteger _privateKey, BigInteger _publicKey,
                       BigInteger _modulo, int _bitLength)
    throws IllegalArgumentException {
        checkBitLength(_bitLength);
        this.privateKey = _privateKey;
        this.publicKey = _publicKey;
        this.modulo = _modulo;
        this.bitLength = _bitLength;
    }

    /**
     * Create a Rsa object using the specified keys, modulo and bitlength.
     */
    public Rsa (byte[] _privateKey, byte[] _publicKey,
                       byte[] _modulo, int _bitLength)
    throws IllegalArgumentException {
        checkBitLength(_bitLength);
        this.privateKey = new BigInteger(_privateKey);
        this.publicKey = new BigInteger(_publicKey);
        this.modulo = new BigInteger(_modulo);
    }

    /**
     * Create a Rsa object and a new key pair with the specified modulo
     * bitlength.
     */
    public Rsa (int _bitLength)
    throws IllegalArgumentException {
        checkBitLength(_bitLength);
        this.bitLength = _bitLength;
        System.err.println("New Rsa with bitLength: " + bitLength);
        generateKeys();
    }

    /**
     * Create a Rsa object and a new key pair with a modulo of bitlength=1024
     */
    public Rsa () {
        this.bitLength = 1024;
        generateKeys();
    }

    /**
     * Check if the given bitlength fits our restrictions.
     */
    private void checkBitLength (int bitLength)
    throws IllegalArgumentException {
        if (bitLength % 8 != 0) {
            throw new IllegalArgumentException(
                    "bitLength must be a multiple of 8.");
        }
    }

    /**
     * Generate publicKey and privateKey.
     */
    private void generateKeys () {
        BigInteger p, q, n, phin, pubkey, privkey, d;
        SecureRandom srandom = new SecureRandom();
        do {
            p = BigInteger.probablePrime(this.bitLength/2, srandom);
            q = BigInteger.probablePrime(this.bitLength/2, srandom);
            n = p.multiply(q);
        } while (n.bitLength() != this.bitLength || p.compareTo(q) == 0);
        phin = p.subtract(BigInteger.ONE).multiply(
                   q.subtract(BigInteger.ONE));

        pubkey = new BigInteger("65537");
        while (!pubkey.gcd(phin).equals(BigInteger.ONE)) {
            pubkey = pubkey.add(BigInteger.ONE);
        }
        privkey = pubkey.modInverse(phin);

        this.modulo = n;
        this.publicKey = pubkey;
        this.privateKey = privkey;

        // We check that the key works since there is a chance that
        // probablePrime gave us a non-prime number
        if (!testKeys()) {
            generateKeys();
        }

        LOG.debug("Modulo bitlength: " + this.modulo.bitLength());
        LOG.debug("Modulo length: " + this.modulo.toByteArray().length);
    }

    /**
     * Return true if the generated keys seems to work correctly false
     * otherwise. This method is public because of the junit tests.
     */
    public boolean testKeys() {
        //return true;
        BigInteger m, c;
        Random random = new Random();

        // FIXME: Is checking just one encryption/decryption enough?
        do {
            m = new BigInteger(this.modulo.bitLength(), random);
            // We have 50% chance of doing the loop again in the
            // worst case scenario.
        } while (!(m.compareTo(this.modulo) < 0));
        c = encrypt(m, this.publicKey, this.modulo);
        c = decrypt(c);
        if (c.compareTo(m) != 0) return false;

        return true;
    }

    /**
     * Encrypt the specified message using the key set with setEncryptKey.
     */
    public BigInteger encrypt(BigInteger msg) {
        return encrypt(msg, this.encryptKey, this.encryptModulo);
    }

    /**
     * Encrypt the specified message with the specified key and the specified
     * modulo.
     */
    public BigInteger encrypt(BigInteger msg, BigInteger key,
                              BigInteger modulo) {
        if (key == null || modulo == null) {
            LOG.error(this, "Encrypt key is not set.");
            return null;
        }
        if (msg.compareTo(modulo) != -1) {
            LOG.error(this, "Message " + msg.toString()
                            + " must be smaller than the modulo "
                            + modulo.toString());
            throw new IllegalArgumentException("msg " + msg 
                                + " must be smaller than the modulo " + modulo);
        }

        BigInteger cypher = msg.modPow(key, modulo);
        return cypher;
    }

    /**
     * Decrypt the specified message with the key set with our private key.
     */
    public BigInteger decrypt(BigInteger cypher) {
        if (this.publicKey == null) {
            // No decrypt key set
            return null;
        }
        if (cypher.compareTo(this.modulo) != -1) {
            LOG.error(this, "Cypher " + cypher.toString()
                            + " must be smaller than the modulo "
                            + modulo.toString());
            //throw new IllegalArgumentException(
            //              "cypher must be smaller than the modulo.");
        }

        BigInteger msg = cypher.modPow(this.privateKey, this.modulo);
        return msg;
    }

    /**
     * Return the public key.
     */
    public byte[] getPublicKey () {
        return this.publicKey.toByteArray();
    }

    /**
     * Return the key used by encrypt(byte[]).
     */
    public byte[] getCryptKey () {
        return this.encryptKey.toByteArray();
    }

    /**
     * Return the modulo of the key pair.
     */
    public byte[] getModulo() {
        return this.modulo.toByteArray();
    }

    /**
     * Return the modulo used by encrypt(byte[])
     */
    public byte[] getEncryptModulo() {
        return this.modulo.toByteArray();
    }

    /**
     * Set the key and modulo used by encrypt(byte[]).
     */
    public void setEncryptKey(byte[] key, byte[] modulo) {
        this.encryptKey = new BigInteger(key);
        this.encryptModulo = new BigInteger(modulo);
        LOG.debug(this, "Encrypt key set to \"" + key
                        + "\" with mod " + modulo);
    }
}
