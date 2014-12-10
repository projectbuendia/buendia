package org.projectbuendia;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author Pim de Witte(wwadewitte), Whitespell LLC
 *         12/10/14
 */
public class GenerateOutput {

    private static final String MAIN_TEMPLATE_PATH = "templates/main_template.json";
    private static final String MAIN_TEMPLATE;

    private static final String PATIENT_TEMPLATE_PATH = "templates/patient_template.json";
    private static final String PATIENT_TEMPLATE;

    private static final String ENCOUNTER_TEMPLATE_PATH = "templates/encounter_template.json";
    private static final String ENCOUNTER_TEMPLATE;

    /* read all templates */
    static {
        String PATIENT_TEMPLATE_TMP, ENCOUNTER_TEMPLATE_TMP, MAIN_TEMPLATE_TMP;
        try {
            PATIENT_TEMPLATE_TMP = FileUtils.readFile(PATIENT_TEMPLATE_PATH, StandardCharsets.UTF_8);
            ENCOUNTER_TEMPLATE_TMP = FileUtils.readFile(ENCOUNTER_TEMPLATE_PATH, StandardCharsets.UTF_8);
            MAIN_TEMPLATE_TMP = FileUtils.readFile(MAIN_TEMPLATE_PATH, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println(e);
            PATIENT_TEMPLATE_TMP = null;
            ENCOUNTER_TEMPLATE_TMP = null;
            MAIN_TEMPLATE_TMP = null;
        }
        PATIENT_TEMPLATE = PATIENT_TEMPLATE_TMP;
        ENCOUNTER_TEMPLATE = ENCOUNTER_TEMPLATE_TMP;
        MAIN_TEMPLATE = MAIN_TEMPLATE_TMP;
    }


    /* generate a specific patient object and it's inner observations */

    private static String generatePatient(String template, int uid, int encounter_amount) {
        if(template.contains("@encounter_template@")) {
               StringBuilder s = new StringBuilder();

                for(int i = 0; i < encounter_amount;i ++){
                    s.append(ENCOUNTER_TEMPLATE + (i == encounter_amount -1 ? "" : ","));
                }
                template = template.replace("@encounter_template@", s.toString());
        }
        return template.replace("@patient_id@", String.valueOf(uid));
    }

    /* generate the overall output *?

     */
    public static void generateOutput(int patients, int encounters) {
        System.out.println("Generating " + patients + " patients with " + encounters + " encounters each");
        StringBuilder patientStringBuilder = new StringBuilder();

        for(int i = 0; i < patients; i++) {
            patientStringBuilder.append(generatePatient(PATIENT_TEMPLATE, i, encounters) + (i == patients - 1 ? "" : ","));
            if(i % 100 == 0 || i == patients - 1) {
                System.out.println("Generating patient " + i);
            }

        }
        String finalOutput;
        finalOutput = MAIN_TEMPLATE.replace("@patients@", patientStringBuilder.toString());
        FileUtils.writeToFile("generator-out.json", finalOutput);
    }

}
