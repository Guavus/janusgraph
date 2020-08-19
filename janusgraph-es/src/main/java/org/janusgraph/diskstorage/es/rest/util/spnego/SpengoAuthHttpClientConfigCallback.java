package org.janusgraph.diskstorage.es.rest.util.spnego;

import org.apache.http.Header;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClientBuilder;
import org.ietf.jgss.GSSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kerb4j.client.SpnegoClient;

import java.io.IOException;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.List;

/**
 * author shekhar.bansal
 **/
public class SpengoAuthHttpClientConfigCallback implements RestClientBuilder.HttpClientConfigCallback {

    private static final Logger logger = LoggerFactory.getLogger(SpengoAuthHttpClientConfigCallback.class);
    private static final String KRB5_DEBUG = "false";
    private static final String KRB5_CONF = "/etc/krb5.conf";

    private String keytabFilePath;
    private String principal;
    private String kerberosSPN;

    public SpengoAuthHttpClientConfigCallback(String principal, String keytabFilePath, String kerberosSPN){
        logger.debug("inside SpengoAuthHttpClientConfigCallback constructor,  principal:: {}, keytabFilePath:: {}, kerberosSPN:: {} ", principal, keytabFilePath, kerberosSPN);
        this.principal = principal;
        this.keytabFilePath = keytabFilePath;
        this.kerberosSPN = kerberosSPN;
    }

    /**
     * Util function to get kerberos authentication token from principal and keytab path
     */
    private String getKerberosAuthorizationHeader(){
        String kerberosAuthorizationHeader = "";
        SpnegoClient spnegoClient = SpnegoClient.loginWithKeyTab(principal, getNewKeytabPathInsideContainer(keytabFilePath));
        System.setProperty("sun.security.krb5.debug", KRB5_DEBUG);
        System.setProperty("java.security.krb5.conf", KRB5_CONF);

        try {
            kerberosAuthorizationHeader = spnegoClient.createAuthroizationHeaderForSPN(kerberosSPN);
        } catch (IOException | PrivilegedActionException | GSSException e) {
            e.printStackTrace();
            logger.error(String.format("Could not login with the provided " +
                "keytab=%s,principal=%s,ESprincipal=%s", keytabFilePath, principal, kerberosSPN));
        }
        return kerberosAuthorizationHeader;
    }

    /**
     * Converts keytab file path to keytab name accessible at worker node.
     *
     * As cdap sync's files added in path : `program.container.dist.jars` set into cdap-sites config, the required
     * keytab file will be available in worker(slave) node with same file name.
     * So `/etc/sercurity/keytabs/cdap.headless.keytab` will be accessible as `cdap.headless.keytab` name
     * Can be seen in workflow application yarn logs :
     * {"name":"cdap.headless.keytab","uri":"hdfs://rafpa001/cdap/twill/workflow.default.testes101.DataPipelineWorkflow
     * /96e71e4f-ad08-4121-8d58-f8834fd1b59e/cdap.headless.keytab.7c03d0b9-0b0d-4a4a-951f-88ceeac9c1b3.keytab",
     * "lastModified":1597240614888,"size":313,"archive":false,"pattern":null}]}}
     * @param keytabPath
     * @return
     */
    private String getNewKeytabPathInsideContainer(String keytabPath) {
        String[] li =  keytabPath.split("/");
        return li[li.length-1];
    }


    @Override
    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
        String kerberosAuthorizationHeader = getKerberosAuthorizationHeader();
        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Authorization", kerberosAuthorizationHeader));
        httpClientBuilder.setDefaultHeaders(headers);
        return httpClientBuilder;
    }
}
