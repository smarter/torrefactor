package torrefactor.util;

import java.util.Random;
import java.security.SecureRandom;
import java.math.BigInteger;

public class Rsa {

    private BigInteger privateKey = null;
    private BigInteger publicKey = null;
    private BigInteger encryptKey = null;
    private BigInteger encryptModulo = null;
    private BigInteger modulo;
    private BigInteger messageMaxLength;
    private int bitLength;

    public Rsa (BigInteger _privateKey, BigInteger _publicKey,
                       BigInteger _modulo, int _bitLength)
    throws IllegalArgumentException {
        checkBitLength(_bitLength);
        this.privateKey = _privateKey;
        this.publicKey = _publicKey;
        this.modulo = _modulo;
        this.bitLength = _bitLength;
    }

    public Rsa (byte[] _privateKey, byte[] _publicKey,
                       byte[] _modulo, int _bitLength)
    throws IllegalArgumentException {
        checkBitLength(_bitLength);
        this.privateKey = new BigInteger(_privateKey);
        this.publicKey = new BigInteger(_publicKey);
        this.modulo = new BigInteger(_modulo);
    }

    public Rsa (int _bitLength)
    throws IllegalArgumentException {
        checkBitLength(_bitLength);
        this.bitLength = _bitLength;
        generateKeys();
    }

    public Rsa () {
        this.bitLength = 1024;
        generateKeys();
    }

    private void checkBitLength (int bitLength)
    throws IllegalArgumentException {
        if (bitLength % 8 != 0) {
            throw new IllegalArgumentException(
                    "bitLength must be a multiple of 8.");
        }
    }

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
    }

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

    public BigInteger encrypt(BigInteger msg) {
        return encrypt(msg, this.encryptKey, this.encryptModulo);
    }

    public BigInteger encrypt(BigInteger msg, BigInteger key, BigInteger modulo) {
        if (key == null || modulo == null) {
            // No encrypt key set
            return null;
        }
        if (msg.compareTo(modulo) != -1) {
            throw new IllegalArgumentException(
                          "msg " + msg + " must be smaller than the modulo " + modulo);
        }

        BigInteger cypher = msg.modPow(key, modulo);
        return cypher;
    }

    public BigInteger decrypt(BigInteger cypher) {
        if (this.publicKey == null) {
            // No decrypt key set
            return null;
        }
        if (cypher.compareTo(this.modulo) != -1) {
            throw new IllegalArgumentException(
                          "cypher must be smaller than the modulo.");
        }

        BigInteger msg = cypher.modPow(this.privateKey, this.modulo);
        return msg;
    }

    public byte[] getPublicKey () {
        return this.publicKey.toByteArray();
    }

    public byte[] getCryptKey () {
        return this.encryptKey.toByteArray();
    }

    public byte[] getModulo() {
        return this.modulo.toByteArray();
    }

    public void setEncryptKey(byte[] key, byte[] modulo) {
        this.encryptKey = new BigInteger(key);
        this.encryptModulo = new BigInteger(modulo);
    }
}
