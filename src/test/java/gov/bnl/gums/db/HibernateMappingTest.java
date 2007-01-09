/*
 * HibernateMappingTest.java
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
public class HibernateMappingTest extends DBTestBase {
    
    public HibernateMappingTest(String testName) {
        super(testName);
    }
    
    public void testCreateRetrieveDelete() throws Exception {
        Session sess = sessions.openSession();
        Transaction trans = sess.beginTransaction();
        HibernateMapping mapping = new HibernateMapping();
        mapping.setDn("/DC=org/DC=doegrids/OU=People/CN=Gabriele Carcassi");
        mapping.setAccount("usatlas1");
        mapping.setMap("test");
        sess.save(mapping);
        Long id = mapping.getId();
        trans.commit();
        trans = sess.beginTransaction();
        HibernateMapping mapping2 = (HibernateMapping) sess.load(HibernateMapping.class, id);
        assertEquals("/DC=org/DC=doegrids/OU=People/CN=Gabriele Carcassi", mapping2.getDn());
        sess.delete(mapping2);
        trans.commit();
        trans = sess.beginTransaction();
        try {
        	HibernateMapping mapping3 = (HibernateMapping) sess.load(HibernateMapping.class, id);
            fail("The object wasn't deleted");
        } catch (ObjectNotFoundException e) {
            // all well
        }
        trans.commit();
        sess.close();
    }
    
}
