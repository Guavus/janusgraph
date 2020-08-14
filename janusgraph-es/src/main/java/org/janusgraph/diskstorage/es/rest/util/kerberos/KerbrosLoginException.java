package org.janusgraph.diskstorage.es.rest.util.kerberos;

/**
 * author shekhar.bansal
 **/
public class KerbrosLoginException extends Exception {

    public KerbrosLoginException(String message) {
        super(message);
    }

    public KerbrosLoginException(String message, Throwable cause) {
        super(message, cause);
    }

    public KerbrosLoginException(Throwable cause) {
        super(cause);
    }
}
