/* 
 *  This file is part of the Torrefactor project
 *  Copyright 2011 Guillaume Martres <smarter@ubuntu.com>
 *  Copyright 2011 Florian Vessaz <florian.vessaz@gmail.com> 
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *      1. Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *      2. Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */

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
