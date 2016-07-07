package ws.wamp.jawampa.connection;

public interface IWampConnectionPromise<T> extends IWampConnectionFuture<T> {

    void fulfill(T value);

    void reject(Throwable error);

    boolean isSuccess();

    Throwable error();
    
    /**
     * A default implementation of the promise whose instance methods do nothing.<br>
     * Can be used in cases where the caller is not interested in the call results.
     */
    public static final IWampConnectionPromise<Void> Empty = new IWampConnectionPromise<Void>() {
        @Override
        public Void result() {
            return null;
        }

        @Override
        public Object state() {
            return null;
        }

        @Override
        public void fulfill(Void value) {
            
        }

        @Override
        public void reject(Throwable error) {
            
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public Throwable error() {
            return null;
        }
    };
}