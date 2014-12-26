package org.projectbuendia;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Pim de Witte(wwadewitte), Whitespell LLC
 *         12/10/14
 *         org.projectbuendia
 *         ${FILE_NAME}
 */
public class FileUtils {

    /**
     * Write a string to a a specified file
     *
     * @param path    The path of the file that needs to be written to
     * @param content The semantic version that is available for update
     */

    public static void writeToFile(String path, String content) {
        Writer writer = null;

        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(path), "utf-8"));
            writer.write(content);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (Exception ex) {
            }
        }
    }

    public static String readFile(String path, Charset encoding)
            throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

}
