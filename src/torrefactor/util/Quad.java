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

package torrefactor.util;

import java.io.*;

/**
 * An immutable class to store a quadruplet of objects.
 */
public class Quad<F, S, T, O> implements Serializable {
    private final F first;
    private final S second;
    private final T third;
    private final O fourth;
    private static final long serialVersionUID = 1L;

    /**
     * Creates a triplet with the following invariants:
     * new Quad(f,s,t,o).first() == f;
     * new Quad(f,s,t,o).second() == s;
     * new Quad(f,s,t,o).third() == t;
     * new Quad(f,s,t,o).quad() == o;
     * @param first The first element of the quadruplet
     * @param second The second element of the quadruplet
     * @param third The third element of the quadruplet
     * @param fourth the fourth element of the quadruplet
     */
    public Quad(F first, S second, T third, O fourth) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
    }

    /**
     * Returns the first element of the quadruplet
     */
    public F first() {
        return this.first;
    }

    /**
     * Returns the second element of the quadruplet
     */
    public S second() {
        return this.second;
    }

    /**
     * Returns the third element of the quadruplet
     */
    public T third() {
        return this.third;
    }

    /**
     * Returns the fourth element of the quadruplet
     */ 
    public O fourth() {
        return this.fourth;
    }
}
