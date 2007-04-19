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
public class ManualGroupRemove extends RemoteCommand {
    static {
        command = new ManualGroupRemove();
    }

    /**
     * Creates a new ManualGroup_Remove object.
     */
    public ManualGroupRemove() {
        syntax = "USERGROUP USERDN1 [USERDN2] ...";
        description = "Removes a user from a manually managed group. " +
            "USERGROUP is the name of the manual user group.";
    }

    protected org.apache.commons.cli.Options buildOptions() {
        Options options = new Options();

        return options;
    }

    protected void execute(org.apache.commons.cli.CommandLine cmd)
        throws Exception {
        if (cmd.getArgs().length < 2) {
            failForWrongParameters("Missing parameters...");
        }

        String userGroup = cmd.getArgs()[0];

        for (int nArg = 2; nArg < cmd.getArgs().length; nArg++) {
            getGums().manualGroupRemove(userGroup, cmd.getArgs()[nArg]);
        }
    }
}
