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

import java.util.concurrent.*;
import java.util.concurrent.Future;

/**
 * An implementation of the Future interface where the result of
 * the computation is set using the set() function.
 *
 * This class is thread-safe.
 *
 * JAVADOC BUG: javadoc 1.6.0_18 crashes on this class because of
 * "implements Future&lt;V&gt;" removing the generic make it work again.
 */
public class FutureSetter<V> implements Future<V> {
    private V value;
    private boolean isDone;
    private boolean isCancelled;

    public FutureSetter() {
        this.value = null;
        this.isDone = false;
        this.isCancelled = false;
    }

    /**
     * Set the value of the object in the Future
     * Once a value has been set, it cannot be changed.
     * This will make all get() calls return and isDone() will be set to true.
     * If set(null) is called, get() will throw  an ExecutionException
     */
    synchronized public void set(V _value) {
        if (this.isDone) {
            return;
        }
        this.value = _value;
        this.isDone = true;
        notifyAll();
    }
    
    /**
     * {@inheritDoc}
     */
    synchronized public V get()
    throws InterruptedException, ExecutionException {
        try {
            return get(0, null);
        } catch (TimeoutException ignored) {
            //Can't happen
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    synchronized public V get(long timeout, TimeUnit unit)
    throws InterruptedException, ExecutionException, TimeoutException {
        long startTime = System.currentTimeMillis();
        long timeoutMs = 0;
        if (timeout != 0) {
            timeoutMs = TimeUnit.MILLISECONDS.convert(timeout, unit);
        }
        while (!this.isDone) {
            wait(timeoutMs);
            if (this.isCancelled) {
                throw new CancellationException();
            }
            if (timeoutMs != 0 && System.currentTimeMillis() - startTime >= timeoutMs) {
                throw new TimeoutException();
            }
        }
        if (value == null) {
            throw new ExecutionException(new Exception("Object value for FutureSetter was null"));
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    synchronized public boolean isDone() {
        return this.isDone;
    }

    /**
     * {@inheritDoc}
     */
    synchronized public boolean isCancelled() {
        return this.isCancelled;
    }

    /**
     * The mayInterruptIfRunning argument is ignored
     */
    synchronized public boolean cancel(boolean mayInterruptIfRunning) {
        if (this.isDone) {
            return false;
        }
        this.isCancelled = true;
        notifyAll();
        return true;
    }
}
