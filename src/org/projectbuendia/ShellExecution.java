package org.projectbuendia;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author Pim de Witte(wwadewitte), Whitespell LLC
 *         11/20/14
 *         control
 */
public class ShellExecution {

    /**
     * Executes a command
     *
     * @param command          The exact command to execute
     * @return exit code integer representation
     */

    public static int executeCommand(String command) {

        StringBuffer output = new StringBuffer();

        Process p = null;
        try {
            System.out.println(command);
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(p.getInputStream()));

                String line = "";
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    output.append(line + "\n");
                }
                reader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return (p == null ? -1 : p.exitValue());

    }
}
