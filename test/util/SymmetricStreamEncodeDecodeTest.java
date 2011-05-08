/* Original file: http://cowww.epfl.ch/proginfo/itpwww/projet/src/
 *                SymmetricStreamEncodeDecodeTest.java
 */

package test.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.*;
import java.util.Arrays;
import java.security.SecureRandom;

import torrefactor.util.XorInputStream;
import torrefactor.util.XorOutputStream;


public class SymmetricStreamEncodeDecodeTest {

    public static void main (String[] args) {
        org.junit.runner.JUnitCore.main(
                "test.util.SymmetricStreamEncodeDecodeTest");
    }
    
    @Test
    public void testEncryptingDecryptingStreamsSingleLine() throws IOException {
        for (int i = 2; i < 11; i++) {
            int keyLength = 1 << i;
            System.out.println("Testing with symmetric key of length (bytes): "
                               + keyLength);
            byte[] symmetricKey = new byte[keyLength];
            (new SecureRandom()).nextBytes(symmetricKey);
            String message = "\u6709\u7406\u7531\u76f8\u4fe1\uff0c\u4ffe"
                             + "\u4eba\u6b96\u6c11\u5316\u4e4b\u524d\u5605"
                             + "\u7f8e\u570b\u672c\u571f\u5605\u4eba\uff0c"
                             + "\u597d\u591a\u90fd\u4fc2\u5927\u6982\u55ba"
                             + "\u4e00\u842c\u81f3\u4e94\u842c\u5e74\u524d"
                             + "\u5de6\u53f3\uff0c\u7531\u4e9e\u6d32\u79fb"
                             + "\u6c11\u904e\u53bb\u5605\u3002\u4f62\u54cb"
                             + "\u5c31\u4fc2\u4eca\u65e5\u5572\u4eba\u6240"
                             + "\u8b1b\u5605\u5370\u7b2c\u5b89\u539f\u4f4f"
                             + "\u6c11\u3002\u4ffe\u6b50\u6d32\u4eba\u6b96"
                             + "\u6c11\u5316\u4e4b\u524d\u5605\u7f8e\u570b"
                             + "\u65e9\u5df2\u7d93\u6709\u597d\u591a\u6587"
                             + "\u5316\u5165\u5497\u53bb\uff0c\u5305\u62ec"
                             + "\u897f\u5357\u9762\u5605\u963f\u90a3\u85a9"
                             + "\u65af\u540c\u6771\u9762\u5605\u963f\u767b"
                             + "\u90a3\u6587\u5316\u7b49\u7b49\u3002\u55ba"
                             + "\u5462\u5572\u6587\u5316\u7576\u4e2d\uff0c"
                             + "\u6709\u90e8\u4efd\u4fc2\u597d\u7740\u91cd"
                             + "\u8981\u5b9a\u5c45\u843d\u569f\uff0c\u540c"
                             + "\u57cb\u8981\u751f\u591a\u5572\u5b50\u5b6b"
                             + "\u569f\u4f5c\u70ba\u52de\u52d5\u529b\u3002"
                             + "\u6240\u4ee5\u65e9\u55ba\u516c\u5143\u524d"
                             + "\u4e8c\u5343\u4e94\u767e\u5e74\uff0c\u7f8e"
                             + "\u570b\u6771\u90e8\u5c31\u5df2\u7d93\u53ef"
                             + "\u4ee5\u9760\u7576\u5730\u571f\u751f\u5605"
                             + "\u592a\u967d\u82b1\u7b49\u7b49\uff0c\u800c"
                             + "\u767c\u5c55\u51fa\u7368\u7acb\u767c\u5c55"
                             + "\u5605\u8fb2\u696d\u3002\u5f8c\u4f86\uff0c"
                             + "\u7531\u58a8\u897f\u54e5\u50b3\u5165\u5605"
                             + "\u4e00\u5572\u7a40\u7269\u54c1\u7a2e\u66f4"
                             + "\u52a0\u6709\u80fd\u529b\u9069\u61c9\u7576"
                             + "\u5730\u5605\u6c23\u5019\uff0c\u6240\u4ee5"
                             + "\u5c31\u53d6\u4ee3\u5497\u672c\u571f\u5605"
                             + "\u7522\u54c1\u3002\n";
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            OutputStream out = new XorOutputStream(
                    buffer,
                    symmetricKey,
                    symmetricKey.length);

            BufferedWriter bufferedWriter = new BufferedWriter(
                    new OutputStreamWriter(out, "UTF-8"));
            bufferedWriter.write(message);
            //bufferedWriter.write("foobar\n");
            bufferedWriter.flush();
            bufferedWriter.close();

            /*assertFalse("The content of the buffer must have been encrypted",
                        Arrays.equals(buffer.toByteArray(),
                                      message.nextBytes("UTF-8")));*/

            InputStream in = new XorInputStream(
                    new  ByteArrayInputStream(buffer.toByteArray()),
                    symmetricKey,
                    symmetricKey.length);

            String line = new BufferedReader(
                    new InputStreamReader(in, "UTF-8")).readLine();
            assertEquals("The original message sent over the output stream "
                         + "must be equal to the message read from the input "
                         + "stream", message, line + "\n");
        }
    }

    @Test
    public void testEncryptingDecryptingStreamsMultiLine() throws IOException {
        for (int i = 2; i < 11; i++) {
            int keyLength = 1 << i;
            byte[] symmetricKey = new byte[keyLength];
            (new SecureRandom()).nextBytes(symmetricKey);
            String firstLine = "First Line \n";
            String secondLine = "Second Line \n";
            String thirdLine = "Third Line \n";

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            OutputStream out = new XorOutputStream(
                    buffer,
                    symmetricKey,
                    symmetricKey.length);

            BufferedWriter bufferedWriter = new BufferedWriter(
                    new OutputStreamWriter(out));
            bufferedWriter.write(firstLine);
            bufferedWriter.write(secondLine);
            bufferedWriter.write(thirdLine);
            //bufferedWriter.write("foobar\n");
            bufferedWriter.flush();
            bufferedWriter.close();

            InputStream in = new XorInputStream(
                    new ByteArrayInputStream(buffer.toByteArray()),
                    symmetricKey,
                    symmetricKey.length);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in));
            String line = reader.readLine();
            assertNotNull(line);
            assertEquals(firstLine, line + "\n");

            line = reader.readLine();
            assertNotNull(line);
            assertEquals(secondLine, line + "\n");

            line = reader.readLine();
            assertNotNull(line);
            assertEquals(thirdLine, line + "\n");

            // FIXME: Why does this next line make java throw a
            //        OutOfMemoryError?
            //line = reader.readLine();
            //assertNull("There must be no extra line to read", line);

            reader.close();
        }
    }

}
