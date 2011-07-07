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
 * A list whose element are set to expire after a certain delay.
 * Expired elements are automatically removed from the list.
 */
public class ExpirationList<E> {
    private long expireAfter; //in milliseconds
    private List<Timestamped<E>> list;

    /**
     * @param expireAfter Time after which an element is considered expired.
     * @param unit        The time unit of the expireAfter argument.
     */
    public ExpirationList(int expireAfter, TimeUnit unit) {
        this.expireAfter = TimeUnit.MILLISECONDS.convert(expireAfter, unit);
        this.list = new LinkedList<Timestamped<E>>();
    }

    /**
     * Adds the specified element to the end of this list.
     * @param e Element to be added to the list.
     */
    public synchronized boolean add(E e) {
            return this.list.add(new Timestamped<E>(e));
        }

    /**
     * Appends all of the elements in the specified collection to the end of this
     * list, in the order that they are returned by the specified collection's
     * iterator (optional operation).
     * @param c Collection containing elements to be added to this list 
     */
    public synchronized boolean addAll(Collection<? extends E> c) {
            for (E elem : c) {
                if (!this.list.add(new Timestamped<E>(elem))) {
                    return false;
                }
            }
            return true;
    }

    /**
     * Returns a copy of the underlying list containing only
     * non expired elements.
     */
    public synchronized List<E> unwrapCopy() {
        List<E> copyList = new LinkedList<E>();
        long curTime = System.currentTimeMillis();
        Iterator<Timestamped<E>> iter = this.list.iterator();
        while (iter.hasNext()) {
            Timestamped<E> elem = iter.next();
            if (curTime - elem.timestamp() > this.expireAfter) {
                iter.remove();
                continue;
            }
            copyList.add(elem.unwrap());
        }
        return copyList;
    }
}

/**
 * An immutable class to represent objects timestamped at their creation.
 */
class Timestamped<E> {
    private final E elem;
    private final long timestamp;

    public Timestamped(E e) {
        this.elem = e;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Returns the time when the object was created,
     * in milliseconds (same format as System.currentTimeMillis())
     */
    public long timestamp() {
        return this.timestamp;
    }

    /**
     * Returns the wrapped object.
     */
    public E unwrap() {
        return this.elem;
    }
}

