/*
 * HibernateUserTest.java
 * JUnit based test
 *
 * Created on October 11, 2004, 1:47 PM
 */

package gov.bnl.gums.db;


import net.sf.hibernate.*;

/**
 *
 * @author carcassi
 */
public class HibernateUserTest extends DBTestBase {
    
    public HibernateUserTest(String testName) {
        super(testName);
    }
    
    public void testCreateRetrieveDelete() throws Exception {
        Session sess = sessions.openSession();
        Transaction trans = sess.beginTransaction();
        HibernateUser user = new HibernateUser();
        user.setDn("/DC=org/DC=doegrids/OU=People/CN=Gabriele Carcassi");
        user.setGroup("test");
        user.setGroup("test");
        sess.save(user);
        Long id = user.getId();
        trans.commit();
        trans = sess.beginTransaction();
        HibernateUser user2 = (HibernateUser) sess.load(HibernateUser.class, id);
        assertEquals("/DC=org/DC=doegrids/OU=People/CN=Gabriele Carcassi", user2.getDn());
        sess.delete(user2);
        trans.commit();
        trans = sess.beginTransaction();
        try {
        	sess.load(HibernateUser.class, id);
            fail("The object wasn't deleted");
        } catch (ObjectNotFoundException e) {
            // all well
        }
        trans.commit();
        sess.close();
    }
    
}
