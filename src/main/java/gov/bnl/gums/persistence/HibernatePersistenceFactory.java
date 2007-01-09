/*
 * HibernatePersistenceFactory.java
 *
 * Created on June 15, 2005, 4:42 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package gov.bnl.gums.persistence;

import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import net.sf.hibernate.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import gov.bnl.gums.db.AccountPoolMapperDB;
import gov.bnl.gums.db.HibernateMappingDB;
import gov.bnl.gums.db.HibernateUserGroupDB;
import gov.bnl.gums.db.ManualAccountMapperDB;
import gov.bnl.gums.db.ManualUserGroupDB;
import gov.bnl.gums.db.UserGroupDB;

/**
 *
 * @author carcassi
 */
public class HibernatePersistenceFactory extends PersistenceFactory {
    private Log log = LogFactory.getLog(HibernatePersistenceFactory.class);
    private SessionFactory sessions;

    public HibernatePersistenceFactory() {
    	log.trace("HibernatePersistenceFactory instanciated");
    }    
    
    public HibernatePersistenceFactory(String name) {
    	super(name);
    	log.trace("HibernatePersistenceFactory instanciated");
    }
    
    public UserGroupDB retrieveUserGroupDB(String name) {
        return new HibernateUserGroupDB(this, name);
    }

    public ManualUserGroupDB retrieveManualUserGroupDB(String name) {
        return new HibernateUserGroupDB(this, name);
    }

    public ManualAccountMapperDB retrieveManualAccountMapperDB(String name) {
        return new HibernateMappingDB(this, name);
    }

    public AccountPoolMapperDB retrieveAccountPoolMapperDB(String name) {
        return new HibernateMappingDB(this, name);
    }
    
    public void setConnectionFromHibernateProperties() {
        try {
            setProperties(readHibernateProperties());
        } catch (MissingResourceException e) {
            throw new RuntimeException("Couldn't find database configuration file (hibernate.properties)", e);
        }
    }
    
    private Properties readHibernateProperties() {
        log.trace("Retrieving hibernate properties from hibernate.properties in the classpath");
        PropertyResourceBundle prop = (PropertyResourceBundle) ResourceBundle.getBundle("hibernate");
        Properties prop2 = new Properties();
        Enumeration keys = prop.getKeys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            prop2.setProperty(key, prop.getString(key));
        }
        return prop2;
    }
    
    public synchronized SessionFactory retrieveSessionFactory() {
        if (sessions != null) return sessions;
        sessions = buildSessionFactory();
        return sessions;
    }
    
    private SessionFactory buildSessionFactory() {
        try {
            log.trace("Creating Hibernate Session Factory with the following properties: " + getProperties());
            net.sf.hibernate.cfg.Configuration cfg = new net.sf.hibernate.cfg.Configuration()
            // Properties for the hibernate session are taken from the properties specified either in the
            // gums.config file, or set programmatically to the class (when unit testing)
                .setProperties(getProperties())
                .addClass(gov.bnl.gums.db.HibernateMapping.class)
                .addClass(gov.bnl.gums.db.HibernateUser.class);
            return cfg.buildSessionFactory();
        } catch (Exception e) {
            log.error("Couldn't initialize Hibernate", e);
            throw new RuntimeException("An error occurred while initializing the database environment (hibernate): "+ e.getMessage(), e);
        }
    }
    
}
