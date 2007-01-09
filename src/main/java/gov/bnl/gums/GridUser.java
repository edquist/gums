/*
 * GridUser.java
 *
 * Created on August 31, 2004, 4:00 PM
 */

package gov.bnl.gums;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** Represent a GRID Identity in GUMS, which is a certificate with its DN and FQAN.
 *
 * @author  Gabriele Carcassi
 */
public class GridUser {
    private Log log = LogFactory.getLog(GridUser.class);
    
    private String certificateDN;
    private FQAN voFQAN;
    
    /**
     * Creates a GRID credentail with no DN and FQAN.
     */
    public GridUser() {
    }
    
    /**
     * Creates a new object representing a Grid credential.
     * @param userDN the DN of the user certificate (i.e. "/DC=org/DC=doegrids/OU=People/CN=Gabriele Carcassi")
     * @param fqan The Fully Qualified Attribute name String representation (i.e. "/atlas/production/Role=Leader")
     */
    public GridUser(String userDN, String fqan) {
        setCertificateDN(userDN);
        if (fqan != null)
            setVoFQAN(new FQAN(fqan));
    }
    
    /**
     * Retrieve the certificate DN of the user.
     * @return The certificate DN (i.e. "/DC=org/DC=doegrids/OU=People/CN=Gabriele Carcassi")
     */
    public String getCertificateDN() {
        return this.certificateDN;
    }
    
    /**
     * Changes the certificate DN for the Grid credential.
     * @param certificateDN A GRID certificate DN (i.e. "/DC=org/DC=doegrids/OU=People/CN=Gabriele Carcassi")
     */
    public void setCertificateDN(String certificateDN) {
        this.certificateDN = certificateDN;
    }
    
    /**
     * Retrieve the VOMS Fully Qualified Attribute name.
     * @return The VOMS FQAN selected with voms-proxy-init (i.e. "/atlas/production/Role=Leader")
     */
    public FQAN getVoFQAN() {
        return this.voFQAN;
    }
    
    /**
     * Sets the VOMS Fully Qualified Attribute name for the credential.
     * @param voFQAN The VOMS FQAN selected with voms-proxy-init (i.e. "/atlas/production/Role=Leader")
     */
    public void setVoFQAN(FQAN voFQAN) {
        this.voFQAN = voFQAN;
    }
    
    /**
     * Changed to reflect the change in equals, as in Object contract.
     * @return A hash created from the DN and FQAN.
     */
    public int hashCode() {
        if (certificateDN != null) {
            return certificateDN.hashCode();
        }
        if (voFQAN != null) {
            return voFQAN.getFqan().hashCode();
        }
        return 0;
    }
    
    /**
     * A GridUser will be equal only to another GridUser with the same DN and FQAN.
     * @param obj another object
     * @return true if the object was a GridUser with equivalent credentials
     */
    public boolean equals(Object obj) {
        GridUser user = (GridUser) obj;
        if ((user.getCertificateDN() == null) ? certificateDN != null : (!user.getCertificateDN().equals(certificateDN))) {
            if (log.isTraceEnabled()) {
                log.trace(this + " !equals " + obj + " for different DN");
            }
            return false;
        }
        if ((user.voFQAN == null) ? voFQAN != null : (!user.voFQAN.equals(voFQAN))) {
            if (log.isTraceEnabled()) {
                log.trace(this + " !equals " + obj + " for dirrent FQAN");
            }
            return false;
        }
        if (log.isTraceEnabled()) {
            log.trace(this + " equals " + obj);
        }
        return true;
    }
    
    /**
     * Returns a legible String representation for the credentail.
     * @return String reprentation of the credential (i.e. "GridID[/DC=org/DC=doegrids/OU=People/CN=Gabriele Carcassi]")
     */
    public String toString() {
        if (voFQAN == null) {
            return "GridID[" + certificateDN + "]";
        }
        return "GridID[" + certificateDN + ", " + voFQAN + "]";
    }
    
}
