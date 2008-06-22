/*
 * BNLPersistenceFactory.java
 *
 * Created on February 3, 2005, 9:52 AM
 */

package gov.bnl.gums.persistence;

import gov.bnl.gums.configuration.Configuration;
import gov.bnl.gums.db.AccountPoolMapperDB;
import gov.bnl.gums.db.ManualAccountMapperDB;
import gov.bnl.gums.db.ManualUserGroupDB;
import gov.bnl.gums.db.UserGroupDB;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Persistence factory instantiation that combines a hibernate persistence factory
 * with a small part of the ldap persistence factory (see addToSecondaryGroup).  When a pool 
 * account is assigned a group, this is set in ldap.
 *
 * @author Gabriele Carcassi, Jay Packard
 */
public class LocalPersistenceFactory extends PersistenceFactory {
    /**
     * Implements AccountPoolMapperDB for the local persistence factory
     * 
     * @author Gabriele Carcassi, Jay Packard
     */
    private class LocalAccountPoolMapperDB implements AccountPoolMapperDB {
        private AccountPoolMapperDB db;
        private String group;
        private List secondaryGroups;
        
        public LocalAccountPoolMapperDB(String poolname, String group, List secondaryGroups) {
            this.group = group;
            this.secondaryGroups = secondaryGroups;
            db = persFactory.retrieveAccountPoolMapperDB(poolname);
        }
        
        public void addAccount(String account) {
            db.addAccount(account);
        }

        public String assignAccount(String userDN) {
            String account = db.assignAccount(userDN);
            if (account == null) return null;
            log.trace("Assigned '" + account + "' to '" + userDN);
            try {
                assignGroups(account);
                return account;
            } catch (RuntimeException e) {
                log.trace("Group assignment failed: unassign user", e);
                unassignUser(userDN);
                log.trace("User unassigned");
                throw e;
            }
        }
        
        public boolean needsCacheRefresh() {
        	return true;
        }
        
        public boolean removeAccount(String account) {
        	return db.removeAccount(account);
        }

        public String retrieveAccount(String userDN) {
            String account = db.retrieveAccount(userDN);
            if (account!=null && synchGroups) {
                assignGroups(account);
            }
            return account;
        }

        public java.util.Map retrieveAccountMap() {
            return db.retrieveAccountMap();
        }
        
        public java.util.Map retrieveReverseAccountMap() {
            return db.retrieveReverseAccountMap();
        }

        public List retrieveUsersNotUsedSince(Date date) {
            return db.retrieveUsersNotUsedSince(date);
        }
        
        synchronized public void setNeedsCacheRefresh(boolean value) {
        }

        public void unassignAccount(String account) {
        	db.unassignAccount(account);
        }
        
        public void unassignUser(String user) {
            db.unassignUser(user);
        }
        
        private void assignGroups(String account) {
            ldap.changeGroupID(account, group);
            log.trace("Assigned '" + group + "' to '" + account);
            Iterator iter = secondaryGroups.iterator();
            while (iter.hasNext()) {
                String group = (String) iter.next();
                ldap.addToSecondaryGroup(account, group);
                log.trace("Assigned secondary group '" + group + "' to '" + account);
            }
        }
        
    }
    
    static public String getTypeStatic() {
		return "local";
	}
    private Log log = LogFactory.getLog(LocalPersistenceFactory.class);    
    private HibernatePersistenceFactory persFactory;
	private LDAPPersistenceFactory ldap;
    
	private boolean synchGroups = false;
	
    /**
     * Create a new local persistence factory.  This empty constructor is needed by the XML Digester.
     */
    public LocalPersistenceFactory() {
    	super();
        persFactory = new HibernatePersistenceFactory();
        ldap = new LDAPPersistenceFactory();   	
    	log.trace("LocalPersistenceFactory instanciated");
    }
    
    /**
     * Create a new local persistence factory with a configuration.
     * 
     * @param configuration
     */
    public LocalPersistenceFactory(Configuration configuration) {
    	super(configuration);
        persFactory = new HibernatePersistenceFactory(configuration);
        ldap = new LDAPPersistenceFactory(configuration);   	
    	log.trace("LocalPersistenceFactory instanciated");
    }
    
    /**
     * Create a new local persistence factory with a configuration and a name.
     * 
     * @param configuration
     * @param name
     */
    public LocalPersistenceFactory(Configuration configuration, String name) {
    	super(configuration, name);
        persFactory = new HibernatePersistenceFactory(configuration, name);
        ldap = new LDAPPersistenceFactory(configuration, name);  
    	log.trace("LocalPersistenceFactory instanciated");
    }
 
    public PersistenceFactory clone(Configuration configuration) {
    	LocalPersistenceFactory persistenceFactory = new LocalPersistenceFactory(configuration, new String(getName()));
    	persistenceFactory.setDescription(new String(getDescription()));
//   	persistenceFactory.setCaCertFile(getCaCertFile());
//    	persistenceFactory.setTrustStorePassword(getTrustStorePassword());
	persistenceFactory.setGroupObject(new String(getGroupObject()));
	persistenceFactory.setPeopleObject(new String(getPeopleObject()));
    	persistenceFactory.setGroupIdField(new String(getGroupIdField()));
    	persistenceFactory.setGroupField(new String(getGroupField());
	persistenceFactory.setAccountField(new String(getAccountField()));
    	persistenceFactory.setMemberAccountField(new String(getMemberAccountField()));
    	persistenceFactory.setProperties((Properties)getProperties().clone());
    	persistenceFactory.setSynchGroups(persistenceFactory.isSynchGroups());
    	return persistenceFactory;
    }
    
    public String getCaCertFile() {
    	return ldap.getCaCertFile();
    }

    public String getAccountField() {
    	return ldap.getAccountField();
    }    
    
    public String getGroupIdField() {
    	return ldap.getGroupIdField();
    }
   
    public String getGroupField() {
        return ldap.getGroupField();
    }

    public String getGroupTree() {
        return ldap.getGroupTree();
    }
 
    public String getMemberAccountField() {
    	return ldap.getMemberAccountField();
    }
    
    public Properties getLDAPProperties() {
        return filterProperties("ldap.");
    }

    public Properties getMySQLProperties() {
        return filterProperties("mysql.");
    }
    
    public String getPeopleTree() {
        return ldap.getTreeObject();
    }

    public String getTrustStorePassword() {
    	return ldap.getTrustStorePassword();
    }
    
    public String getType() {
		return "local";
	}
    
    /**
     * Getter for property synchGroups.
     * @return Value of property synchGroups.
     */
    public boolean isSynchGroups() {
        return this.synchGroups;
    }

    public AccountPoolMapperDB retrieveAccountPoolMapperDB(String name) {
        StringTokenizer tokens = new StringTokenizer(name, ".");
        if (!tokens.hasMoreTokens()) {
            return persFactory.retrieveAccountPoolMapperDB(name);
        }
        String pool = tokens.nextToken();
        if (!tokens.hasMoreTokens()) {
            return persFactory.retrieveAccountPoolMapperDB(name);
        }
        String group = tokens.nextToken();
        List secondaryGroups = new ArrayList();
        while (tokens.hasMoreTokens()) {
            secondaryGroups.add(tokens.nextToken());
        }
        return new LocalAccountPoolMapperDB(pool, group, secondaryGroups);
    }
    
    public ManualAccountMapperDB retrieveManualAccountMapperDB(String name) {
        return persFactory.retrieveManualAccountMapperDB(name);
    }
    
    public ManualUserGroupDB retrieveManualUserGroupDB(String name) {
        return persFactory.retrieveManualUserGroupDB(name);
    }
    
    public UserGroupDB retrieveUserGroupDB(String name) {
        return persFactory.retrieveUserGroupDB(name);
    }
    
    public void setCaCertFile(String caCertFile) {
        ldap.setCaCertFile( caCertFile );
    }
    
    public void setConfiguration(Configuration configuration) {
    	super.setConfiguration(configuration);
    }
        
    public void setAccountField(String accountField) {
    	ldap.setAccountField(accountField);
    }
    
    public void setMemberAccountField(String memberAccountField) {
    	ldap.setMemberAccountField(memberAccountField);
    }
    
    public void setGroupIdField(String groupIdField) {
    	ldap.setGroupIdField(groupIdField);
    }
   
    public void setGroupField(String groupField) {
        ldap.setGroupField(groupField);
    }

    public void setGroupTree(String groupTree) {
        ldap.setGroupTree(groupTree);
    }
 
    public void setName(String name) {
    	super.setName(name);
    }

    public void setPeopleTree(String peopleTree) {
        ldap.setPeopleTree(peopleTree);
    }

    public void setProperties(Properties properties) {
        super.setProperties(properties);
        persFactory.setProperties(getMySQLProperties());
        ldap.setProperties(getLDAPProperties());
    }
    
    public void setSynchGroups(boolean synchGroups) {
    	this.synchGroups = synchGroups;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        ldap.setTrustStorePassword(trustStorePassword);
    }    
    
	public String toXML() {
    	String retStr = "\t\t<localPersistenceFactory\n"+
    		"\t\t\tname='"+getName()+"'\n"+
    		"\t\t\tdescription='"+getDescription()+"'\n"+
    		"\t\t\tsynchGroups='"+synchGroups+"'\n"+
//    		"\t\t\tcaCertFile='"+getCaCertFile()+"'\n"+
//    		"\t\t\ttrustStorePassword='"+getTrustStorePassword()+"'\n"+
                "\t\t\tgroupObject='"+getGroupObject()+"'\n"+
                "\t\t\tpeopleObject='"+getPeopleObject()+"'\n"+		
    		"\t\t\tgroupIdField='"+getGroupIdField()+"'\n"+
    		"\t\t\tgroupField='"+getGroupField()+"'\n"+
		"\t\t\taccountField='"+getAccountField()+"'\n"+
		"\t\t\tmemberAccountField='"+getMemberAccountField()+"'\n";
    	
    	Iterator keyIt = getProperties().keySet().iterator();
    	while(keyIt.hasNext()) {
    		String key = (String)keyIt.next();
    		retStr += "\t\t\t"+key+"='"+getProperties().getProperty(key)+"'\n";
    	}

    	if (retStr.charAt(retStr.length()-1)=='\n')
    		retStr = retStr.substring(0, retStr.length()-1);    	
    	
    	retStr += "/>\n\n";
    	
    	return retStr;
	}
    
    private Properties filterProperties(String prefix) {
        Properties filtered = new Properties();
        Enumeration keys = getProperties().propertyNames();
        while (keys.hasMoreElements()) {
            String name = (String) keys.nextElement();
            if (name.startsWith(prefix)) {
                filtered.setProperty(name.substring(prefix.length()), getProperties().getProperty(name));
            }
        }
        return filtered;
    }
   
}
