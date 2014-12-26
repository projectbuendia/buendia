package org.projectbuendia;


import java.util.Scanner;

/**
 * @author Pim de Witte(wwadewitte), Whitespell LLC
 *         12/6/14
 */
public class TakeInput {

    public static void takeInput() {
        {
            String patients, encounters;

            java.util.Scanner input = new Scanner(System.in);

            System.out.println("Starting Buendia Generator....");
            System.out.println("How many patients do you want to generate?");
            patients = input.nextLine();

            System.out.println("How many encounters per patient?");
            encounters = input.nextLine();

            GenerateOutput.generateOutput(Integer.parseInt(patients), Integer.parseInt(encounters));

        }
    }
}
