/*
 * GUMS2MapUser.java
 *
 * Created on June 9, 2004, 1:44 PM
 */
package gov.bnl.gums.admin;

import org.apache.commons.cli.*;

/**
 * @author carcassi
 */
public class ManualMappingRemove extends RemoteCommand {
    static {
        command = new ManualMappingRemove();
    }

    /**
     * Creates a new ManualMapping_Remove object.
     */
    public ManualMappingRemove() {
        syntax = "ACCOUNTMAPPER USERDN";
        description = "Maps a DN to a user in a manually managed mapping. " +
            "ACCOUNTMAPPER is the name of the manual account mapper.";
    }

    protected org.apache.commons.cli.Options buildOptions() {
        Options options = new Options();

        return options;
    }

    protected void execute(org.apache.commons.cli.CommandLine cmd)
        throws Exception {
        if (cmd.getArgs().length < 3) {
            failForWrongParameters("Missing parameters...");
        }

        String accountMapper = cmd.getArgs()[0];
        String userDN = cmd.getArgs()[1];

        getGums().manualMappingRemove(accountMapper, userDN);
    }
}
