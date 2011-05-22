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
        long time = System.currentTimeMillis();
        return this.list.remove(0).get(timeout, TimeUnit.MILLISECONDS);
    }
}
