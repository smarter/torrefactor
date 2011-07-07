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

import java.util.*;
import java.util.concurrent.*;

/**
 * A list to store Future tasks and retrieve their values.
 */
public class FutureList<E> {
    private List<Future<E>> list;
    public FutureList() {
        this.list = new LinkedList<Future<E>>();
    }

    /**
     * Appends the specified element to the end of this list.
     */
    public boolean add(Future<E> e) {
        if (e.isCancelled()) {
            return false;
        }
        return this.list.add(e);
    }

    /**
     * Returns the number of elements in this list.
     */
    public int size() {
        return this.list.size();
    }

    /**
     * Returns true if this list contains no elements.
     */
    public boolean isEmpty() {
        return this.list.size() == 0;
    }

    /**
     * Block until a future is done, then remove it from the list
     * and return it. If cancelled tasks are found during the execution of
     * this function, they will be removed from the list.
     *
     * @param timeout   Number of milliseconds to wait before throwing
     *                  TimeoutException
     *
     * @return          The result of a computation or null if the list
     *                  of Future is empty
     */
    public E removeAny(int timeout) throws TimeoutException, ExecutionException {
        if (list.isEmpty()) {
            return null;
        }
        long time = System.currentTimeMillis();
        while (System.currentTimeMillis() < time + timeout) {
            Iterator<Future<E>> iter = this.list.iterator();
            while (iter.hasNext()) {
                Future<E> future = iter.next();
                if (future.isCancelled()) {
                    iter.remove();
                    continue;
                }
                if (future.isDone()) {
                    iter.remove();
                    try {
                        return future.get();
                    } catch (InterruptedException ignored) {
                        //can't happen since isDone() == true
                        return null;
                    }
                }
            }
            try {
                Thread.sleep(10); //No point in checking more often than that
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        throw new TimeoutException();
    }

    /**
     * Block until the first future added to the list is done or cancelled.
     * Then remove it from the list and return its result.
     *
     * @param timeout Number of milliseconds to wait before throwing
     *                TimeoutException
     *
     * @return        The result of a computation or null if the list of Future
     *                is empty
     */
    public E removeFirst(int timeout)
    throws TimeoutException, ExecutionException, InterruptedException {
        if (this.list.isEmpty()) {
            return null;
        }
        return this.list.remove(0).get(timeout, TimeUnit.MILLISECONDS);
    }
}
