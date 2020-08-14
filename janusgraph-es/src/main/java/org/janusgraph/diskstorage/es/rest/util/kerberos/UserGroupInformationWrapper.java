package org.janusgraph.diskstorage.es.rest.util.kerberos;

import com.kerb4j.common.jaas.sun.Krb5LoginContext;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * author shekhar.bansal
 **/
public class UserGroupInformationWrapper {

    private static final Logger logger = LoggerFactory.getLogger(UserGroupInformationWrapper.class);

    private UserGroupInformation ugi;

    public UserGroupInformationWrapper() {
        try {
            ugi = UserGroupInformation.getLoginUser();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Subject getSubject(String principal, String keytabFilePath){
        logger.info("inside get subject, principal:: {} , keytabFilePath:: {}", principal, keytabFilePath);
        Subject subject = getSubjectFromUGI();
        if(subject == null) {
            logger.info("failed to obtain subject from UGI, trying to login with keytab");
            subject = getSubjectFromKeyTabAndPrincipal(principal, keytabFilePath);
        } else {
            logger.info("using subject from UGI");
        }
        return subject;
    }


    private Subject getSubjectFromKeyTabAndPrincipal(String principal, String keytabFilePath) {
        LoginContext loginContext = Krb5LoginContext.loginWithKeyTab(principal, keytabFilePath);
        return loginContext.getSubject();
    }

    private Subject getSubjectFromUGI() {
        if (ugi==null) {
            logger.warn("ugi is null");
            return null;
        }
        try {
            logger.debug("ugi user:: " + ugi.getUserName());
            Method method = ugi.getClass().getDeclaredMethod("getSubject", null);
            method.setAccessible(true);
            Subject subject = (Subject) method.invoke(ugi, null);
            return subject;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }
}