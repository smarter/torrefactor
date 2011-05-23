package torrefactor.util;

import java.util.concurrent.*;
import java.util.concurrent.Future;

/**
 * An implementation of the Future interface where the result of
 * the computation is set using the set() function.
 *
 * This class is thread-safe.
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
