package test.util;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

//import crypto.KeyGenerator;
import torrefactor.util.Rsa;


public class RsaTest {

    public static void main (String[] strings) {
        org.junit.runner.JUnitCore.main("test.util.RsaTest");
    }

    /**
     * This test is too simple and obvious, but it shows how to test expected
     * exceptions.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testBadModulusSize(){
        new Rsa (new BigInteger("3233"),
                 new BigInteger("17"),
                 new BigInteger("2753"),
                 20);
    }

    @Test
    public void testKeyPair() {
        Rsa keyPair;

        keyPair = new Rsa(new BigInteger("2753"), new BigInteger("17"),
                                 new BigInteger("3233"), 16);
        keyPair.setEncryptKey(keyPair.getPublicKey(), keyPair.getModulo());
        BigInteger encrypted = keyPair.encrypt(new BigInteger("65"));
        assertEquals(2790, encrypted.intValue());
        assertEquals(65, keyPair.decrypt(encrypted).intValue());

        encrypted = keyPair.encrypt(new BigInteger("123"));
        assertEquals(855, encrypted.intValue());
        assertEquals(123, keyPair.decrypt(encrypted).intValue());

        keyPair = new Rsa(new BigInteger("11787"), new BigInteger("3"),
                                 new BigInteger("17947"), 16);
        keyPair.setEncryptKey(keyPair.getPublicKey(), keyPair.getModulo());
        encrypted = keyPair.encrypt(new BigInteger("513"));
        assertEquals(8363, encrypted.intValue());
        assertEquals(513, keyPair.decrypt(encrypted).intValue());

        encrypted = keyPair.encrypt(new BigInteger("14313"));
        assertEquals(13366, encrypted.intValue());
        assertEquals(14313, keyPair.decrypt(encrypted).intValue());
    }

    @Test
    public void testKeys() {
        Rsa keyPair;

        keyPair = new Rsa(new BigInteger("2753"), new BigInteger("17"),
                                 new BigInteger("3233"), 16);
        keyPair.setEncryptKey(keyPair.getPublicKey(), keyPair.getModulo());
        BigInteger encrypted = keyPair.encrypt(new BigInteger("65"));
        assertTrue(keyPair.testKeys());
    }

    @Test
    public void testEncryptDecrypt() throws Exception {
        String message = "A pigeon has no use for keys";

        for (int pow = 3; pow < 11; pow++) {
            int N = 1 << pow;
            System.out.println("Testing with N = " + N);
            Rsa keyPair = new Rsa(N);
            keyPair.setEncryptKey(keyPair.getPublicKey(), keyPair.getModulo());
            byte[] bytes = message.getBytes("UTF-8");

            BigInteger[] encrypted = encrypt(keyPair, bytes);
            byte[] bytesDecrypted = decrypt(keyPair, encrypted);

            System.out.println("message  = " + message);

            String decryptedMessage = new String(bytesDecrypted, "UTF-8");
            System.out.println("encrypted and decrypted message = "
                               + decryptedMessage);

            assertEquals("The original message must be equal to the"
                         + "encrypted-decrypted message",
                         decryptedMessage, message);
        }
    }


    private BigInteger[] encrypt(Rsa keyPair, byte[] input)
    throws UnsupportedEncodingException {
        BigInteger[] output = new BigInteger[input.length];

        for (int i = 0; i < input.length; i++) {
            output[i] = keyPair.encrypt(
                            new BigInteger(Integer.toString(input[i])));
        }

        return output;
    }

    private byte[] decrypt(Rsa keyPair, BigInteger[] input)
    throws UnsupportedEncodingException {
        byte[] output = new byte[input.length];

        for (int i = 0; i < input.length; i++) {
            output[i] = keyPair.decrypt(input[i]).byteValue();
        }

        return output;
    }
}
