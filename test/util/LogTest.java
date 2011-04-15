package test.util;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import torrefactor.util.Log;

public class LogTest {

    public static void main (String[] strings) {
        org.junit.runner.JUnitCore.main("test.util.LogTest");
    }

    @Test
    public void logTest() {
        Log log = Log.getInstance();
        String foo = "foo";
        Integer i = new Integer (42);
        log.log(Log.INFO, foo, "Logged message about foo.");
        log.log(Log.INFO, i, "i is: " + i);
        System.err.println(" --- Now printing ring --- ");
        log.printRing();
        for (int j=0; j < 200; j++) {
            log.log(Log.INFO, this, Integer.toString(j));
        }
    }
//
//    @Test
//    public void intTest() {
//        Log log = Log.getInstance();
//        log.debug("intTest", 42);
//        log.info("intTest", 42);
//        log.warning("intTest", 42);
//        log.error("intTest", 42);
//    }
//
//    @Test
//    public void doubleTest() {
//        Log log = Log.getInstance();
//        log.debug("intTest", 84L);
//        log.info("intTest", 84L);
//        log.warning("intTest", 84L);
//        log.error("intTest", 84L);
//    }
}
