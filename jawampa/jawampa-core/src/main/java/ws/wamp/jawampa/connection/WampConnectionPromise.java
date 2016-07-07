package ws.wamp.jawampa.connection;

/**
 * A lightweight promise type which is used for the implementation
 * of WAMP connection adapters.<br>
 * The promise is not thread-safe and supports no synchronization.<br>
 * This means it is not allowed to call fulfill or reject more than once and
 * to access the result without external synchronization.
 */
public class WampConnectionPromise<T> implements IWampConnectionPromise<T> {
    Throwable error = null;
    T result = null;
    Object state;
    ICompletionCallback<T> callback;
    
    /**
     * Creates a new promise
     * @param callback The callback which will be invoked when {@link WampConnectionPromise#fulfill(Object)}
     * or {@link WampConnectionPromise#reject(Throwable)} is called.
     * @param state An arbitrary state object which is stored inside the promise.
     */
    public WampConnectionPromise(ICompletionCallback<T> callback, Object state) {
        this.callback = callback;
        this.state = state;
    }
    
    @Override
    public Object state() {
        return state;
    }
    
    @Override
    public void fulfill(T value) {
        this.result = value;
        callback.onCompletion(this);
    }
    
    @Override
    public void reject(Throwable error) {
        this.error = error;
        callback.onCompletion(this);
    }
    
    /**
     * Resets a promises state.<br>
     * This can be used to reuse a promise.<br>
     * This may only be used if it's guaranteed that the promise and the
     * associated future is no longer used by anyone else.
     */
    public void reset(ICompletionCallback<T> callback, Object state) {
        this.error = null;
        this.result = null;
        this.callback = callback;
        this.state = state;
    }

    @Override
    public boolean isSuccess() {
        return error == null;
    }

    @Override
    public T result() {
        return result;
    }

    @Override
    public Throwable error() {
        return error;
    }
}