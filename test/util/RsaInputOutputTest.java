package test.util;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.*;
import java.util.*;
import java.math.BigInteger;

//import crypto.KeyGenerator;
import torrefactor.util.*;


public class RsaInputOutputTest {

    public static void main (String[] strings) {
        org.junit.runner.JUnitCore.main("test.util.RsaInputOutputTest");
    }

    @Test
    public void InputOutputTest () throws Exception {
        
        File file = new File("data/RsaInputOutputTest.dat");
        if (file.exists()) file.delete();
        file.deleteOnExit();
        file.createNewFile();

        FileInputStream input = new FileInputStream(file);
        FileOutputStream output = new FileOutputStream(file);

        Rsa rsa = new Rsa();
        rsa.setEncryptKey(rsa.getPublicKey(), rsa.getModulo());

        RsaInputStream rsaInput = new RsaInputStream(input, rsa);
        RsaOutputStream rsaOutput = new RsaOutputStream(output, rsa);
        byte[] array0 = {1, 123};
        //byte[] array0 = {25, 123, 78, 22, 0, 12, 84, 78, 90, 48, 54};
        byte[] array1 = new byte[array0.length];
        
        rsaOutput.write(array0);
        rsaOutput.flush();

        rsaInput.read(array1, 0, array1.length);

        assertTrue(Arrays.equals(array0, array1));
    }
}
