package org.janusgraph.diskstorage.es.rest.util.kerberos;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * author shekhar.bansal
 **/
public class KerberosCredentialsProvider implements CredentialsProvider {

    private static final Logger logger = LoggerFactory.getLogger(KerberosCredentialsProvider.class);
    private AuthScope authScope;
    private Credentials credentials;

    @Override
    public void setCredentials(
        final AuthScope authScope,
        final Credentials credentials) {
        Args.notNull(authScope, "Authentication scope");
        Args.notNull(credentials, "Credentials");
        this.authScope = authScope;
        this.credentials = credentials;
    }

    @Override
    public Credentials getCredentials(final AuthScope authscope) {
        Args.notNull(authscope, "Authentication scope");
        if (authscope.match(this.authScope)>=0) {
            return this.credentials;
        }
        logger.warn("credentials not found for authscope: {}", authscope);
        return null;
    }

    @Override
    public void clear() {
        this.credentials = null;
        this.authScope = null;
    }

}
