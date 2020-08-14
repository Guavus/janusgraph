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

        logger.debug("attempting to get subject from storage's UGI");
        Subject subject = getSubjectFromStorageUGI();
        if(subject != null) {
            logger.info("using subject from storage's UGI");
            return subject;
        }

        logger.debug("attempting to get subject from ES's UGI");
        subject = getSubjectFromKeyTabAndPrincipalUGI(principal, keytabFilePath);
        if(subject != null) {
            logger.info("using ES's UGI's subject");
            return subject;
        }
        throw new KerbrosLoginException("Error kerberos login");
    }

    private Subject getSubjectFromKeyTabAndPrincipalUGI(String principal, String keytabFilePath){
        try {
            if (this.ugi == null) {
                UserGroupInformation.setConfiguration(new Configuration());
                UserGroupInformation ugi = UserGroupInformation.loginUserFromKeytabAndReturnUGI(principal, keytabFilePath);
                UserGroupInformation.setLoginUser(ugi);
                this.ugi = ugi;
            }
            return getSubjectFromUGI(ugi);
        } catch (IOException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            logger.warn("error occurred while logging using UGI " , e);
            return null;
        }
    }

    private Subject getSubjectFromStorageUGI() {
        try {
            return getSubjectFromUGI(UserGroupInformation.getLoginUser());
        } catch (IOException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            logger.warn("error occurred while getting login user from UGI " , e);
            return null;
        }
    }

    private Subject getSubjectFromUGI(UserGroupInformation ugi) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (ugi==null) {
            logger.warn("ugi is null");
            return null;
        }
        logger.debug("ugi user:: " + ugi.getUserName());
        Method method = ugi.getClass().getDeclaredMethod("getSubject", null);
        method.setAccessible(true);
        Subject subject = (Subject) method.invoke(ugi, null);
        return subject;

    }
}