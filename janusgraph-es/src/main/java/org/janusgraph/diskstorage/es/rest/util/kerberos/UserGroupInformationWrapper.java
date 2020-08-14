package org.janusgraph.diskstorage.es.rest.util.kerberos;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * author shekhar.bansal
 **/
public class UserGroupInformationWrapper {

    private static final Logger logger = LoggerFactory.getLogger(UserGroupInformationWrapper.class);

    private static UserGroupInformation ugi = null;

    /**
     * Method to obtain subject
     * First attempt is made by using storage's UGI
     * On failure, second attempt is made by using ES keytab, principal
     *
     * @param principal User principal name
     * @param keytabFilePath path to keytab file for user
     *
     * @return {@link Subject}
     * @throws KerbrosLoginException
     */
    public Subject getSubject(String principal, String keytabFilePath) throws KerbrosLoginException {
        logger.info("inside get subject, principal:: {} , keytabFilePath:: {}", principal, keytabFilePath);
        initialiseUGI(principal, keytabFilePath);

        return getSubjectFromUGI();
    }

    private void initialiseUGI(String principal, String keytabFilePath) throws KerbrosLoginException {
        try {
            UserGroupInformation ugiObj = UserGroupInformation.getLoginUser();
            if(ugiObj != null){
                ugi = ugiObj;
                logger.info("using storage's UGI");
                return;
            }
        } catch (IOException e) {
            logger.warn("error occurred while getting login user from UGI " , e);
        }

        try {
            UserGroupInformation.setConfiguration(new Configuration());
            UserGroupInformation ugiObj = UserGroupInformation.loginUserFromKeytabAndReturnUGI(principal, keytabFilePath);
            UserGroupInformation.setLoginUser(ugiObj);
            ugi = ugiObj;
            logger.info("using ES's UGI");
        } catch (IOException e) {
            logger.warn("error occurred while creating UGI " , e);
            throw new KerbrosLoginException("Error creating UGI", e);
        }
    }

    private Subject getSubjectFromUGI() throws KerbrosLoginException {
        logger.debug("ugi user:: " + ugi.getUserName());
        try {
            Method method  = ugi.getClass().getDeclaredMethod("getSubject", null);
            method.setAccessible(true);
            Subject subject = (Subject) method.invoke(ugi, null);
            return subject;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new KerbrosLoginException("Error kerberos login", e);
        }
    }
}