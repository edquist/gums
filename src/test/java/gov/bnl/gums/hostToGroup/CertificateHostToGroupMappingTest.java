/*
 * CertificateHostGroupTest.java
 * JUnit based test
 *
 * Created on May 10, 2005, 4:13 PM
 */

package gov.bnl.gums.hostToGroup;

import junit.framework.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 *
 * @author carcassi
 */
public class CertificateHostToGroupMappingTest extends TestCase {
    CertificateHostToGroupMapping group;
    
    public CertificateHostToGroupMappingTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        group = new CertificateHostToGroupMapping();
    }

    protected void tearDown() throws Exception {
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(CertificateHostToGroupMappingTest.class);
        
        return suite;
    }

    public void testAnyServiceOnMachine() {
        group.setCn("*/www.atlasgrid.bnl.gov");
        assertTrue(group.isInGroup("/DC=org/DC=doegrids/OU=Services/CN=grid-monitoring/www.atlasgrid.bnl.gov"));
        assertTrue(group.isInGroup("/DC=org/DC=doegrids/OU=Services/CN=host/www.atlasgrid.bnl.gov"));
        assertFalse(group.isInGroup("/DC=org/DC=doegrids/OU=Services/CN=host/atlasgrid.bnl.gov"));
        assertFalse(group.isInGroup("/DC=org/DC=doegrids/OU=Services/CN=host/atlasgrid13.usatlas.bnl.gov"));
        assertFalse(group.isInGroup("/DC=org/DC=doegrids/OU=Services/CN=atlasgrid13.usatlas.bnl.gov"));
        assertFalse(group.isInGroup("/DC=org/DC=doegrods/OU=Services/CN=atlasgrid13.usatlas.bnl.gov"));
    }

    public void testAnyHostService() {
        group.setCn("host/*,host/*.*,host/*.*.*,host/*.*.*.*");
        assertFalse(group.isInGroup("/DC=org/DC=doegrids/OU=Services/CN=grid-monitoring/www.atlasgrid.bnl.gov"));
        assertTrue(group.isInGroup("/DC=org/DC=doegrids/OU=Services/CN=host/www.atlasgrid.bnl.gov"));
        assertTrue(group.isInGroup("/DC=org/DC=doegrids/OU=Services/CN=host/atlasgrid.bnl.gov"));
        assertTrue(group.isInGroup("/DC=org/DC=doegrids/OU=Services/CN=host/atlasgrid13.usatlas.bnl.gov"));
        assertFalse(group.isInGroup("/DC=org/DC=doegrids/OU=Services/CN=atlasgrid13.usatlas.bnl.gov"));
        assertFalse(group.isInGroup("/DC=org/DC=doegrods/OU=Services/CN=atlasgrid13.usatlas.bnl.gov"));
    }

    public void testUsatlasHosts() {
        group.setCn("*.usatlas.bnl.gov");
        assertFalse(group.isInGroup("/DC=org/DC=doegrids/OU=Services/CN=grid-monitoring/www.atlasgrid.bnl.gov"));
        assertFalse(group.isInGroup("/DC=org/DC=doegrids/OU=Services/CN=host/www.atlasgrid.bnl.gov"));
        assertFalse(group.isInGroup("/DC=org/DC=doegrids/OU=Services/CN=host/atlasgrid.bnl.gov"));
        assertFalse(group.isInGroup("/DC=org/DC=doegrids/OU=Services/CN=host/atlasgrid13.usatlas.bnl.gov"));
        assertTrue(group.isInGroup("/DC=org/DC=doegrids/OU=Services/CN=atlasgrid13.usatlas.bnl.gov"));
        assertTrue(group.isInGroup("/DC=org/DC=doegrods/OU=Services/CN=atlasgrid13.usatlas.bnl.gov"));
    }

    public void testDnAnyServiceOnMachine() {
        group.setDn("/DC=org/DC=doegrids/OU=Services/CN=*/www.atlasgrid.bnl.gov");
        assertTrue(group.isInGroup("/DC=org/DC=doegrids/OU=Services/CN=grid-monitoring/www.atlasgrid.bnl.gov"));
        assertTrue(group.isInGroup("/DC=org/DC=doegrids/OU=Services/CN=host/www.atlasgrid.bnl.gov"));
        assertFalse(group.isInGroup("/DC=org/DC=doegrids/OU=Services/CN=host/atlasgrid.bnl.gov"));
        assertFalse(group.isInGroup("/DC=org/DC=doegrids/OU=Services/CN=host/atlasgrid13.usatlas.bnl.gov"));
        assertFalse(group.isInGroup("/DC=org/DC=doegrids/OU=Services/CN=atlasgrid13.usatlas.bnl.gov"));
        assertFalse(group.isInGroup("/DC=org/DC=doegrods/OU=Services/CN=atlasgrid13.usatlas.bnl.gov"));
    }

    public void testDnAnyHostService() {
        group.setDn("/DC=org/DC=doegrids/OU=Services/CN=host/*,/DC=org/DC=doegrids/OU=Services/CN=host/*.*,/DC=org/DC=doegrids/OU=Services/CN=host/*.*.*,/DC=org/DC=doegrids/OU=Services/CN=host/*.*.*.*");
        assertFalse(group.isInGroup("/DC=org/DC=doegrids/OU=Services/CN=grid-monitoring/www.atlasgrid.bnl.gov"));
        assertTrue(group.isInGroup("/DC=org/DC=doegrids/OU=Services/CN=host/www.atlasgrid.bnl.gov"));
        assertTrue(group.isInGroup("/DC=org/DC=doegrids/OU=Services/CN=host/atlasgrid.bnl.gov"));
        assertTrue(group.isInGroup("/DC=org/DC=doegrids/OU=Services/CN=host/atlasgrid13.usatlas.bnl.gov"));
        assertFalse(group.isInGroup("/DC=org/DC=doegrids/OU=Services/CN=atlasgrid13.usatlas.bnl.gov"));
        assertFalse(group.isInGroup("/DC=org/DC=doegrods/OU=Services/CN=atlasgrid13.usatlas.bnl.gov"));
    }

    public void testDnUsatlasHosts() {
        group.setDn("/DC=org/DC=doegrids/OU=Services/CN=*.usatlas.bnl.gov");
        assertFalse(group.isInGroup("/DC=org/DC=doegrids/OU=Services/CN=grid-monitoring/www.atlasgrid.bnl.gov"));
        assertFalse(group.isInGroup("/DC=org/DC=doegrids/OU=Services/CN=host/www.atlasgrid.bnl.gov"));
        assertFalse(group.isInGroup("/DC=org/DC=doegrids/OU=Services/CN=host/atlasgrid.bnl.gov"));
        assertFalse(group.isInGroup("/DC=org/DC=doegrids/OU=Services/CN=host/atlasgrid13.usatlas.bnl.gov"));
        assertTrue(group.isInGroup("/DC=org/DC=doegrids/OU=Services/CN=atlasgrid13.usatlas.bnl.gov"));
        assertFalse(group.isInGroup("/DC=org/DC=doegrods/OU=Services/CN=atlasgrid13.usatlas.bnl.gov"));
    }
    
}
