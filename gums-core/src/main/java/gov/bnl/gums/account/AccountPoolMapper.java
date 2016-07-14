/*
 * AccountPoolMapper.java
 *
 * Created on June 16, 2004, 3:10 PM
 */

package gov.bnl.gums.account;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Comparator;

import org.apache.log4j.Logger;

import gov.bnl.gums.GUMS;
import gov.bnl.gums.GridUser;
import gov.bnl.gums.AccountInfo;
import gov.bnl.gums.configuration.Configuration;
import gov.bnl.gums.db.AccountPoolMapperDB;

/** 
 * Provides the mapping by assigning user accounts from a pool provided by
 * the AccountPoolMapperDB.
 * <p>
 * The accounts are mapped when the mapUser function is called for that
 * particular user. Accounts are never deleted from the pool
 *
 * @todo should implement caching?
 * @author  Gabriele Carcassi, Jay Packard
 */
public class AccountPoolMapper extends AccountMapper {
	static public String getTypeStatic() {
		return "pool";
	}
    
	private Logger gumsAdminLog = Logger.getLogger(GUMS.gumsAdminLogName);
	private Logger log = Logger.getLogger(AccountPoolMapper.class);
    private AccountPoolMapperDB db;
    private String persistenceFactory = "";
	private String accountPool = "";
    private String groupName = "";
    private boolean recyclable = false;
    private int expiry = 60;
	private static Map assignments = Collections.synchronizedMap(new HashMap());
    
    public AccountPoolMapper() {
    	super();
    }
 
    public AccountPoolMapper(Configuration configuration) {
    	super(configuration);
    }
    
    public AccountPoolMapper(Configuration configuration, String name) {
    	super(configuration, name);
    }
    
    public AccountMapper clone(Configuration configuration) {
    	AccountPoolMapper accountMapper = new AccountPoolMapper(configuration, new String(getName()));
    	accountMapper.setDescription(new String(getDescription()));
    	accountMapper.setAccountPool(new String(accountPool));
    	accountMapper.setPersistenceFactory(new String(persistenceFactory));
        accountMapper.setGroupName(new String(groupName));
        accountMapper.setRecyclable(recyclable);
        accountMapper.setExpiration(expiry);
    	return accountMapper;
    }
    
    public String getAccountPool() {
    	return accountPool;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getAccountPoolRoot() {
    	int index = accountPool.indexOf(".");
    	if (index != -1)
    		return accountPool.substring(0, index);
    	else
    		return accountPool;
    }

    public AccountPoolMapperDB getDB() {
    	if (db==null) {
    		db = getConfiguration().getPersistenceFactory(persistenceFactory).retrieveAccountPoolMapperDB(accountPool, recyclable);
        }
    	return db;
    }

    public List<? extends MappedAccountInfo> getAccountInfo() {
        return getDB().retrieveAccountInfo();
    }

	// helper class for getAssignments()
	class AccountRootStats {
		public int total = 0;
		public int assigned = 0;
		public ArrayList<String> numbers = new ArrayList<String>();
	}

	// Comparator class to sort 700-799,7000-7999, instead of purely ascii order
	class LenComp implements Comparator<String> {
		@Override
		public int compare(String a, String b) {
			int lcomp = a.length() - b.length();
			return lcomp != 0 ? lcomp : a.compareTo(b);
		}
	}

	/**
	 * @return String representation of how many accounts are assigned in database for each root account
	 */
	public String getAssignments() {
		if (getDB().needsCacheRefresh()) {
			log.trace("Refreshing assignments string for account pool mapper "+getName());

			String retStr = new String();

			Map accountReverseMap = getDB().retrieveReverseAccountMap();
			TreeMap<String, AccountRootStats> accountRoots = new TreeMap<String, AccountRootStats>();
			Iterator it = accountReverseMap.keySet().iterator();
			while (it.hasNext()) {
				String account = (String)it.next();
				String accountRoot = getRoot(account);
				String accountNumber = getNumber(account);
				AccountRootStats stats = accountRoots.get(accountRoot);
				if (stats==null) {
					stats = new AccountRootStats();
				}
				stats.total += 1;
				if (!accountReverseMap.get(account).equals(""))
					stats.assigned += 1;
				stats.numbers.add(accountNumber); // number list
				accountRoots.put(accountRoot, stats);
			}
			it = accountRoots.keySet().iterator();
			while (it.hasNext()) {
				String accountRoot = (String)it.next();
				retStr += accountRoot;
				AccountRootStats stats = accountRoots.get(accountRoot);
				List numbers = stats.numbers;
				Collections.sort(numbers, new LenComp());
				Iterator numIt = numbers.iterator();
				String lastNumber = null;
				while(numIt.hasNext()) {
					String number = (String)numIt.next();
					if(lastNumber==null)
						retStr += number;
					else if(greaterThanOne(lastNumber, number))
						retStr += "-" + lastNumber + "," + number;
					else if(!numIt.hasNext())
						retStr += "-" + number;
					lastNumber = number;
				}
				retStr += "(" +
					stats.assigned + "/" +
					stats.total + ")";
				if (it.hasNext())
					retStr += ", ";
			}
			getDB().setCacheRefreshed();
			assignments.put(getDB().getMap(), retStr);
			return retStr;
		}
		else {
			return (String)assignments.get(getDB().getMap());
		}
	}

    public String getPersistenceFactory() {
       return persistenceFactory;
    }
        
    public String getType() {
		return "pool";
	}

    @Override
    public AccountInfo mapUser(GridUser user, boolean createIfDoesNotExist) {
        String account = getDB().retrieveAccount(user);
        String group = groupName == null || groupName.equals("") ? null : groupName;
        if (account != null) return new AccountInfo(account, group);
        if (createIfDoesNotExist) {
        	String newAccount = getDB().assignAccount(user);
        	if (newAccount==null) {
        		String message = "Could not assign user '"+user.getCertificateDN()+"' to account within pool account mapper '"+getName()+"'.";
        		gumsAdminLog.warn(message);
        		GUMS.gumsAdminEmailLog.put("noPoolAccounts", message, false);
        	}
        	return new AccountInfo(newAccount, group);
        }
        else
        	return null;
    }
    
    public void setAccountPool(String accountPool) {
    	this.accountPool = accountPool;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public void setPersistenceFactory(String persistenceFactory) {
        this.persistenceFactory = persistenceFactory;
    }

    public void setRecyclable(boolean recyclable) {
        this.recyclable = recyclable;
    }

    public boolean getRecyclable() {
        return recyclable;
    }

    public void setExpiration(int days) {
        this.expiry = days;
    }

    public int getExpiration() {
        return expiry;
    }

    public void cleanAccounts() {
        if (recyclable && (expiry > 0)) {
            getDB().cleanAccounts(this.expiry);
        }
    }

    public String toString(String bgColor) {
    	return "<td bgcolor=\""+bgColor+"\"><a href=\"accountMappers.jsp?command=edit&name=" + getName() + "\">" + getName() + "</a></td><td bgcolor=\""+bgColor+"\">" + getType() + "</td><td bgcolor=\""+bgColor+"\">" + getAssignments() + "</td>";
    }
    
    public String toXML() {
        String retStr =
            "\t\t<accountPoolMapper\n"+
            "\t\t\tname='"+getName()+"'\n"+
            "\t\t\tdescription='"+getDescription()+"'\n"+
            "\t\t\tpersistenceFactory='"+persistenceFactory+"'\n" +
            "\t\t\trecyclable='"+recyclable+"'\n" +
            "\t\t\texpiration='"+expiry+"'\n" +
            "\t\t\taccountPool='"+accountPool+"'";
        if (groupName != null && !groupName.equals(""))
            retStr += "\n\t\t\tgroupName='"+groupName+"'";
        retStr += "/>\n\n";
        return retStr;
    }
    
    private static String getRoot(String account) {
    	int i = account.length()-1;
    	while (i>=0 && account.charAt(i)>=48 && account.charAt(i)<=57)
    		i--;
    	i++;
    	return new String( account.substring(0,i) );
    }
    
    private static String getNumber(String account) {
    	int i = account.length()-1;
    	while (i>=0 && account.charAt(i)>=48 && account.charAt(i)<=57)
    		i--;
    	i++;
    	return new String( account.substring(i) );
    }    
    
    private static boolean greaterThanOne(String smaller, String larger) {
    	if (smaller.length() != larger.length())
    		return true;
    	int i1 = Integer.parseInt(smaller);
    	int i2 = Integer.parseInt(larger);
    	return (i2 > i1+1);
    }
}
