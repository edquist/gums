/*
 * LDAPPersistenceFactory.java
 *
 * Created on January 21, 2005, 9:37 AM
 */

package gov.bnl.gums.persistence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import gov.bnl.gums.db.AccountPoolMapperDB;
import gov.bnl.gums.db.LDAPGroupIDAssigner;
import gov.bnl.gums.db.LDAPMappingDB;
import gov.bnl.gums.db.LDAPUserGroupDB;
import gov.bnl.gums.db.ManualAccountMapperDB;
import gov.bnl.gums.db.ManualUserGroupDB;
import gov.bnl.gums.db.UserGroupDB;
import gov.bnl.gums.*;

/**
 *
 * @author carcassi
 */
public class LDAPPersistenceFactory extends PersistenceFactory {
    
    private Log log = LogFactory.getLog(LDAPPersistenceFactory.class);
    private Log adminLog = LogFactory.getLog(GUMS.resourceAdminLog);
    
    private String defaultGumsOU = "ou=GUMS";
    private String updateGIDdomains;
    private List domains;
    private boolean synchGroups;
    private List contexts = Collections.synchronizedList(new LinkedList());// *** LDAP connection pool management

    public LDAPPersistenceFactory() {
    }    
    
    public LDAPPersistenceFactory(String name) {
    	super(name);
    }
    
    /** Create a new LDAP DirContext based on the configuration.
     * 
     * @return a new LDAP DirContext
     */
    protected DirContext createLDAPContext() {
        try {
            log.info("Trying to create LDAP connection with properties: " + getProperties());
            return new InitialLdapContext(getProperties(), null);
        } catch (NamingException e) {
            log.warn("Couldn't create LDAP connection: " + e.getMessage() + " - parameters: " + getProperties(), e);
            throw new RuntimeException("Couldn't create LDAP connection: " + e.getMessage());
        }
    }
    
    /** Retrieves an LDAP DirContext from the pool, if available and still valid,
     * or creates a new DirContext if none are found.
     * 
     * @return an LDAP DirContext
     */
    public DirContext retrieveContext() {
        DirContext context;
        while (contexts.size() != 0) {
            context = (DirContext) contexts.remove(0);
            if (isContextValid(context)) {
                log.trace("Using LDAP connection from pool " + context);
                return context;
            }
        }
        context = createLDAPContext();
        log.trace("New LDAP connection created " + context);
        return context;
    }
    
    private boolean skipReleaseContext = false;
    
    /** Returns the LDAP DirContext to the pool, so that it can be reused.
     * 
     * @param context the LDAP context to be returned
     */
    public void releaseContext(DirContext context) {
        if (skipReleaseContext) {
            skipReleaseContext = false;
            return;
        }
        contexts.add(0, context);
        log.trace("LDAP connection returned to pool " + context);
    }
    
    private boolean isContextValid(DirContext context) {
        try {
            context.search(getDefaultGumsOU(), "(map=*)", null);
            return true;
        } catch (Exception e) {
            log.trace("Removing stale LDAP connection from pool " + context, e);
            adminLog.warn("LDAP connection test failed, discarding connection from pool: " + e.getMessage());
            return false;
        }
    }
    
    /** Returns a Context ready to be used (taken from the pool).
     * This is the entry point for the pool, and it can be used
     * by test cases to prepare the LDAP server.
     * 
     * @return an LDAP context
     */
    public DirContext getLDAPContext() {
        return retrieveContext();
    }
    
    // *** PersistenceFactory methods
    
    public UserGroupDB retrieveUserGroupDB(String name) {
        log.trace("Creating LDAP UserGroupDB '" + name + "'");
        return new LDAPUserGroupDB(this, name);
    }

    public ManualUserGroupDB retrieveManualUserGroupDB(String name) {
        log.trace("Creating LDAP ManualUserGroupDB '" + name + "'");
        return new LDAPUserGroupDB(this, name);
    }

    public ManualAccountMapperDB retrieveManualAccountMapperDB(String name) {
        log.trace("Creating LDAP ManualAccountMapperDB '" + name + "'");
        return new LDAPMappingDB(this, name);
    }

    public AccountPoolMapperDB retrieveAccountPoolMapperDB(String name) {
        StringTokenizer tokens = new StringTokenizer(name, ".");
        if (!tokens.hasMoreTokens()) {
            log.trace("Creating LDAP AccountPoolMapperDB '" + name + "' (no GIDs)");
            return new LDAPMappingDB(this, name);
        }
        
        String pool = tokens.nextToken();
        if (!tokens.hasMoreTokens()) {
            log.trace("Creating LDAP AccountPoolMapperDB '" + name + "' (no GIDs)");
            return new LDAPMappingDB(this, name);
        }
        
        String group = tokens.nextToken();
        List secondaryGroups = new ArrayList();
        while (tokens.hasMoreTokens()) {
            secondaryGroups.add(tokens.nextToken());
        }
        
        log.trace("Creating LDAP AccountPoolMapperDB '" + name + "' primary group '" + group + "' secondary groups '" + secondaryGroups + "'");
        return new LDAPMappingDB(this, pool, group, secondaryGroups);
    }
    
    // *** LDAP gid assigner
    LDAPGroupIDAssigner assigner;
    /** Returns the gid assigner for the given ldap, with the configured
     * ldap domains for the factory.
     * 
     * @return an LDAPGroupIDAssigner preconfigured and ready to use
     */
    public LDAPGroupIDAssigner retrieveAssigner() {
        if (assigner == null) {
            assigner = new LDAPGroupIDAssigner(this, domains);
            log.trace("New LDAPGroupsIDAssigner created " + assigner);
        }
        return assigner;
    }

    // *** Factory configuration (getters and setters)

    /**
     * Changes the GUMS DN in which the GUMS objects will be placed.
     * This DN will be relative to the DN as specified in the LDAP url
     * within the LDAP connection parameters.
     * @return the GUMS base DN (defaults to "ou=GUMS")
     */
    public String getDefaultGumsOU()   {

        return this.defaultGumsOU;
    }

    /**
     * Changes the GUMS DN in which the GUMS objects will be placed.
     * This DN will be relative to the DN as specified in the LDAP url
     * within the LDAP connection parameters.
     * @param baseDN the GUMS base DN (defaults to "ou=GUMS")
     */
    public void setDefaultGumsOU(String defaultGumsOU)   {

        this.defaultGumsOU = defaultGumsOU;
    }

    /**
     * Changes the list of domains to update when an account of the pool is 
     * assigned. The values required are a comma separated set of domain DNs
     * in the LDAP server. The domain DNs must be relative to the baseDN
     * specified in the LDAP url within the LDAP connection parameters.
     * <p>
     * If set to null, no gid update will be performed.
     * @return domains for updating the GID (i.e. "dc=usatlas")
     */
    public String getUpdateGIDdomains() {
        return this.updateGIDdomains;
    }

    /**
     * Changes the list of domains to update when an account of the pool is 
     * assigned. The values required are a comma separated set of domain DNs
     * in the LDAP server. The domain DNs must be relative to the baseDN
     * specified in the LDAP url within the LDAP connection parameters.
     * <p>
     * If set to null, no gid update will be performed.
     * @param updateGIDdomains domains for updating the GID (i.e. "dc=usatlas")
     */
    public void setUpdateGIDdomains(String updateGIDdomains) {
        assigner = null;
        this.updateGIDdomains = updateGIDdomains;
    }

    /**
     * This property forces the gid update for account pools at every access.
     * It's handy for when gids gets out of synch.
     * @return if true gids are updated every time accounts from the pool are returned.
     */
    public boolean isSynchGroups() {
        return this.synchGroups;
    }

    /**
     * This property forces the gid update for account pools at every access.
     * It's handy for when gids gets out of synch.
     * @param synchGroups if true gids are updated every time accounts from the pool are returned.
     */
    public void setSynchGroups(boolean synchGroups) {
        this.synchGroups = synchGroups;
    }

    /**
     * Sets the list of properties to be used to connect to LDAP, that is
     * to create the JNDI context.
     * @param properties a set of JNDI properties
     */
    public void setProperties(Properties properties) {
        if (properties.getProperty("java.naming.factory.initial") == null) {
            properties.put("java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory");
        }
    	super.setProperties(properties);
    }

    /**
     * Retrieves the JNDI properties to be used to create the LDAP context
     * by looking at the ldap.properties file in the classpath. This is mainly
     * used for unit testing.
     */
    public void setConnectionFromLdapProperties() {
        try {
            setProperties(readLdapProperties());
        } catch (MissingResourceException e) {
            throw new RuntimeException("Couldn't find LDAP configuration file (ldap.properties)", e);
        }
    }
    
    private Properties readLdapProperties() {
        log.trace("Retrieving LDAP properties from ldap.properties in the classpath");
        PropertyResourceBundle prop = (PropertyResourceBundle) ResourceBundle.getBundle("ldap");
        Properties prop2 = new Properties();
        Enumeration keys = prop.getKeys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            prop2.setProperty(key, prop.getString(key));
        }
        return prop2;
    }
    
    // *** Maps management    
    
    /** Creates a new "map=mapName" entry in the LDAP GUMS tree.
     * 
     * @param mapName the name of the map (i.e. "usatlasSpecialMap")
     * @param mapDN the map DN (i.e. "map=usatlasSpecialMap")
     */
    public void createMap(String mapName, String mapDN) {
        DirContext context = retrieveContext();
        try {
            Attributes atts = new BasicAttributes();
            Attribute oc = new BasicAttribute("objectclass");
            oc.add("GUMStruct");
            oc.add("GUMSMap");
            Attribute map = new BasicAttribute("map", mapName);
            atts.put(oc);
            atts.put(map);
            context.createSubcontext(mapDN , atts);
            log.trace("Created LDAP map '" + mapName + "' at '" + mapDN + "'");
        } catch (Exception e) {
            log.info("LDAPPersistence error - createMap - map '" + mapName + "'", e);
            throw new RuntimeException("Couldn't create LDAP map '" + mapName + "': " + e.getMessage(), e);
        } finally {
            releaseContext(context);
        }
    }
    
    /** Deletes the "map=mapName" map in the LDAP GUMS tree. Will completely
     * delete the map.
     * 
     * @param mapName the name of the map (i.e. "usatlasSpecialMap")
     * @param mapDN the map DN (i.e. "map=usatlasSpecialMap")
     */
    public void destroyMap(String mapName, String mapDN) {
        DirContext context = retrieveContext();
        try {
            SearchControls ctrls = new SearchControls();
            ctrls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            NamingEnumeration result = context.search(mapDN, "(objectclass=*)", ctrls);
            while (result.hasMore()) {
                SearchResult res = (SearchResult) result.next();
                if ("".equals(res.getName().trim())) continue;
                context.destroySubcontext(res.getName() + "," + mapDN);
            }
            context.destroySubcontext(mapDN);
            log.trace("Destroyed LDAP map '" + mapName + "' at '" + mapDN + "'");
        } catch (Exception e) {
            log.info("LDAPPersistence error - destroyMap - map '" + mapName + "'", e);
            throw new RuntimeException("Couldn't destroy LDAP map '" + mapName + "': " + e.getMessage(), e);
        } finally {
            releaseContext(context);
        }
    }
    
    /** Adds a userDN -> account mapping entry in the "map=mapName" LDAP map.
     * 
     * @param userDN the certificate DN of the user (i.e. "/DC=org/DC=doegrids/OU=People/CN=Gabriele Carcassi 12345")
     * @param account the account to whith to map the DN (i.e. "carcassi")
     * @param mapName the name of the map (i.e. "usatlasSpecialMap")
     * @param mapDN the map DN (i.e. "map=usatlasSpecialMap")
     */
    public void addMapEntry(String userDN, String account, String mapName, String mapDN) {
        DirContext context = retrieveContext();
        try {
            try {
                ModificationItem[] mods = new ModificationItem[1];
                mods[0] = new ModificationItem(context.ADD_ATTRIBUTE,
                    new BasicAttribute("user", userDN));
                context.modifyAttributes("account=" + account + "," + mapDN, mods);
                log.trace("Added user '" + userDN + "' / account '" + account + "' to map '" + mapName + "' at '" + mapDN + "' (account entry present)");
            } catch (NameNotFoundException e) {
                Attributes atts = new BasicAttributes();
                Attribute oc = new BasicAttribute("objectclass");
                oc.add("GUMStruct");
                oc.add("GUMSAccount");
                Attribute userAtt = new BasicAttribute("user", userDN);
                Attribute accountAtt = new BasicAttribute("account", account);
                atts.put(oc);
                atts.put(userAtt);
                atts.put(accountAtt);
                context.createSubcontext("account=" + account + "," + mapDN , atts);
                log.trace("Added user '" + userDN + "' / account '" + account + "' to map '" + mapName + "' at '" + mapDN + "' (account entry created)");
            }
        } catch (Exception e) {
            log.info("LDAPPersistence error - addMapEntry - user '" + userDN + "' / account '" + account + "' to map '" + mapName + "' at '" + mapDN + "'", e);
            throw new RuntimeException("Couldn't add mapping to LDAP map - user '" + userDN + "' / account '" + account + "' to map '" + mapName + "' at '" + mapDN + "': " + e.getMessage(), e);
        } finally {
            releaseContext(context);
        }
    }
    
    /** Creates an account in the map "map=mapName", without having a userDN: this is useful
     * for pools of accounts.
     * 
     * @param account the account to whith to map the DN (i.e. "grid0001")
     * @param mapName the name of the map (i.e. "usatlasSpecialMap")
     * @param mapDN the map DN (i.e. "map=usatlasSpecialMap")
     */
    public void createAccountInMap(String account, String mapName, String mapDN) {
        DirContext context = retrieveContext();
        try {
            Attributes atts = new BasicAttributes();
            Attribute oc = new BasicAttribute("objectclass");
            oc.add("GUMStruct");
            oc.add("GUMSAccount");
            Attribute accountAtt = new BasicAttribute("account", account);
            atts.put(oc);
            atts.put(accountAtt);
            context.createSubcontext("account=" + account + "," + mapDN , atts);
            log.trace("Added account '" + account + "' to map '" + mapName + "' at '" + mapDN + "'");
        } catch (Exception e) {
            log.info("LDAPPersistence error - createAccountInMap - account '" + account + "' to map '" + mapName + "' at '" + mapDN + "'", e);
            throw new RuntimeException("Couldn't add account to LDAP map - account '" + account + "' to map '" + mapName + "' at '" + mapDN + "': " + e.getMessage(), e);
        } finally {
            releaseContext(context);
        }
    }
    
    /** Removes a userDN -> acount mapping entry in the "map=mapName LDAP map.
     * It will only remove the user entry, while leaving the account entry.
     * 
     * @param userDN the certificate DN of the user (i.e. "/DC=org/DC=doegrids/OU=People/CN=Gabriele Carcassi 12345")
     * @param mapName the name of the map (i.e. "usatlasSpecialMap")
     * @param mapDN the map DN (i.e. "map=usatlasSpecialMap")
     * @return false if no mapping was removed
     */
    public boolean removeMapEntry(String userDN, String mapName, String mapDN) {
        DirContext context = retrieveContext();
        boolean deleted = false;
        try {
            SearchControls ctrls = new SearchControls();
            ctrls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            NamingEnumeration result = context.search(mapDN, "(user={0})", new Object[] {userDN}, ctrls);
            while (result.hasMore()) {
                SearchResult res = (SearchResult) result.next();
                if ("".equals(res.getName().trim())) continue;
                ModificationItem[] mods = new ModificationItem[1];
                mods[0] = new ModificationItem(context.REMOVE_ATTRIBUTE,
                    new BasicAttribute("user", userDN));
                context.modifyAttributes(res.getName() + "," + mapDN, mods);
                deleted = true;
                log.trace("Removed map entry - user '" + userDN + "' to map '" + mapName + "' at '" + mapDN + "'");
            }
            return deleted;
        } catch (Exception e) {
            log.info("LDAPPersistence error - removeMapEntry - user '" + userDN + "' to map '" + mapName + "' at '" + mapDN + "'", e);
            throw new RuntimeException("Couldn't remove map entry from LDAP map - user '" + userDN + "' to map '" + mapName + "' at '" + mapDN + "': " + e.getMessage(), e);
        } finally {
            releaseContext(context);
        }
    }

    // *** User group management
    
    /** Creates a new "group=groupName" entry in the LDAP GUMS tree.
     * 
     * @param groupName the name of the group (i.e. "usatlas")
     * @param groupDN the group DN (i.e. "group=usatlas")
     */
    public void createUserGroup(String groupName, String groupDN) {
        DirContext context = retrieveContext();
        try {
            Attributes atts = new BasicAttributes();
            Attribute oc = new BasicAttribute("objectclass");
            oc.add("GUMStruct");
            oc.add("GUMSGroup");
            Attribute group = new BasicAttribute("group", groupName);
            atts.put(oc);
            atts.put(group);
            context.createSubcontext(groupDN , atts);
            log.trace("Created user group '" + groupName + "' at '" + groupDN + "'");
        } catch (Exception e) {
            log.info("LDAPPersistence error - createUserGroup - group '" + groupName + "'", e);
            throw new RuntimeException("Couldn't create LDAP group '" + groupName + "': " + e.getMessage(), e);
        } finally {
            releaseContext(context);
        }
    }

    /** Adds a certificate DN to the group "group=groupName".
     * 
     * @param userDN the certificate DN of the user (i.e. "/DC=org/DC=doegrids/OU=People/CN=Gabriele Carcassi 12345")
     * @param groupName the name of the group (i.e. "usatlas")
     * @param groupDN the group DN (i.e. "group=usatlas")
     */
    public void addUserGroupEntry(String userDN, String groupName, String groupDN) {
        DirContext context = retrieveContext();
        try {
            ModificationItem[] mods = new ModificationItem[1];
            mods[0] = new ModificationItem(context.ADD_ATTRIBUTE,
                new BasicAttribute("user", userDN));
            context.modifyAttributes(groupDN, mods);
            log.trace("Added user '" + userDN + "' to group '" + groupName + "' at '" + groupDN + "'");
        } catch (Exception e) {
            log.info("LDAPPersistence error - addUserGroupEntry - user '" + userDN + "' to group '" + groupName + "' at '" + groupDN + "'", e);
            throw new RuntimeException("Couldn't add user to LDAP group - user '" + userDN + "' to group '" + groupName + "' at '" + groupDN + "': " + e.getMessage(), e);
        } finally {
            releaseContext(context);
        }
    }

    /** Removes a certificate DN to the group "group=groupName".
     * 
     * @param userDN the certificate DN of the user (i.e. "/DC=org/DC=doegrids/OU=People/CN=Gabriele Carcassi 12345")
     * @param groupName the name of the group (i.e. "usatlas")
     * @param groupDN the group DN (i.e. "group=usatlas")
     */
    public void removeUserGroupEntry(String userDN, String groupName, String groupDN) {
        DirContext context = retrieveContext();
        try {
            ModificationItem[] mods = new ModificationItem[1];
            mods[0] = new ModificationItem(context.REMOVE_ATTRIBUTE,
                new BasicAttribute("user", userDN));
            context.modifyAttributes(groupDN, mods);
            log.trace("Removed user '" + userDN + "' to group '" + groupName + "' at '" + groupDN + "'");
        } catch (Exception e) {
            log.info("LDAPPersistence error - removeUserGroupEntry - user '" + userDN + "' to group '" + groupName + "' at '" + groupDN + "'", e);
            throw new RuntimeException("Couldn't remove user to LDAP group  - user '" + userDN + "' to group '" + groupName + "' at '" + groupDN + "': " + e.getMessage(), e);
        } finally {
            releaseContext(context);
        }
    }
    
    // *** gid management
    
    private String findGID(String domain, String groupname) {
        DirContext context = retrieveContext();
        try {
            DirContext subcontext = getDomainContext(context, domain);
            NamingEnumeration result = subcontext.search("ou=Group", "(cn={0})", new Object[] {groupname}, null);
            String gid = null;
            if (result.hasMore()) {
                SearchResult item = (SearchResult) result.next();
                Attributes atts = item.getAttributes();
                Attribute gidAtt = atts.get("gidNumber");
                if (gidAtt != null) {
                    gid = (String) gidAtt.get();
                }
            }
            log.trace("Found gid '" + gid + "' for group '" + groupname + "' at '" + domain + "'");
            return gid;
        } catch (Exception e) {
            log.info("Couldn't retrieve gid for '" + groupname + "' at '" + domain + "'", e);
            throw new RuntimeException("Couldn't retrieve gid for '" + groupname + "' at '" + domain + "': " + e.getMessage(), e);
        } finally {
            releaseContext(context);
        }
    }
    
    private void updateGID(String domain, String username, String gid) {
        DirContext context = retrieveContext();
        try {
            DirContext subcontext = getDomainContext(context, domain);
            ModificationItem[] mods = new ModificationItem[1];
            mods[0] = new ModificationItem(subcontext.REPLACE_ATTRIBUTE,
                new BasicAttribute("gidNumber", gid));
            
            subcontext.modifyAttributes("uid="+username+",ou=People", mods);
            log.trace("Changed primary gid for user '" + username + "' to gid '" + gid + "' at '" + domain + "'");
        } catch (Exception e) {
            log.info("Couldn't change gid for user '" + username + "' to gid '" + gid + "' at '" + domain + "'", e);
            throw new RuntimeException("Couldn't change gid for user '" + username + "' to gid '" + gid + "' at '" + domain + "': " + e.getMessage(), e);
        } finally {
            releaseContext(context);
        }
    }
    
    /** Changes the primary gid for the given username. It expects the base DN
     * provided by the LDAP connection url to point to a domain (i.e. "dc-usatlas,dc=bnl,dc=gov")
     * that will contain a "ou=People" and a "ou=Group" subtree.
     * 
     * @param username the username to change the primary group (i.e. "carcassi")
     * @param groupname the primary group name (i.e. "usatlas")
     */
    public void changeGroupID(String username, String groupname) {
        String gid = findGID(null, groupname);
        if (gid == null) {
            throw new RuntimeException("GID for group '" + groupname + "' wasn't found.");
        }
        updateGID(null, username, gid);
    }
    
    /** Changes the primary gid for the given username. It expects the domain to be
     * relative to the base DN
     * provided by the LDAP connection url and to point to a domain (i.e. "dc-usatlas,dc=bnl,dc=gov")
     * that will contain a "ou=People" and a "ou=Group" subtree.
     *
     * @param domain the domain DN relative to be base DN
     * @param username the username to change the primary group (i.e. "carcassi")
     * @param groupname the primary group name (i.e. "usatlas")
     */
    public void changeGroupID(String domain, String username, String groupname) {
        String gid = findGID(domain, groupname);
        if (gid == null) {
            throw new RuntimeException("GID for group '" + groupname + "' wasn't found.");
        }
        updateGID(domain, username, gid);
    }
    
    /** Adds the username to the given secondary group. It expects the base DN
     * provided by the LDAP connection url to point to a domain (i.e. "dc-usatlas,dc=bnl,dc=gov")
     * that will contain a "ou=People" and a "ou=Group" subtree.
     * 
     * @param username the username to add to the secondary group (i.e. "carcassi")
     * @param groupname the secondary group name (i.e. "usatlas")
     */
    public void addToSecondaryGroup(String username, String groupname) {
        addToSecondaryGroup(null, username, groupname);
    }
    
    /** Adds the username to the given secondary group. It expects the domain to be
     * relative to the base DN
     * provided by the LDAP connection url and to point to a domain (i.e. "dc-usatlas,dc=bnl,dc=gov")
     * that will contain a "ou=People" and a "ou=Group" subtree.
     * 
     * @param domain the domain DN relative to be base DN
     * @param username the username to add to the secondary group (i.e. "carcassi")
     * @param groupname the secondary group name (i.e. "usatlas")
     */
    public void addToSecondaryGroup(String domain, String username, String groupname) {
        DirContext context = retrieveContext();
        try {
            DirContext subcontext = getDomainContext(context, domain);
            SearchControls ctrls = new SearchControls();
            ctrls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            NamingEnumeration result = subcontext.search("cn=usatlas, ou=Group", "(memberUid={0})", new Object[] {username}, ctrls);
            if (result.hasMore()) return;
            ModificationItem[] mods = new ModificationItem[1];
            mods[0] = new ModificationItem(subcontext.ADD_ATTRIBUTE,
                new BasicAttribute("memberUid", username));
            
            subcontext.modifyAttributes("cn="+groupname+",ou=Group", mods);
            log.trace("Added secondary group to user - user '" + username + "' to group '" + groupname + "' at '" + domain + "'");
        } catch (Exception e) {
            log.info("Couldn't add user to secondary group - user '" + username + "' to group '" + groupname + "' at '" + domain + "'", e);
            throw new RuntimeException("Couldn't add user to secondary group - user '" + username + "' to group '" + groupname + "' at '" + domain + "': " + e.getMessage(), e);
        } finally {
            releaseContext(context);
        }
    }
    
    private DirContext getDomainContext(DirContext context, String domain) throws Exception {
        if (domain == null) return context;
        return (DirContext) context.lookup(domain);
    }

}
