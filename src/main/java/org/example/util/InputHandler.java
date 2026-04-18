package org.example.util;
import java.util.Scanner;

//Validate User Input
public class InputHandler {
    private final Scanner scanner;

    public InputHandler() {
        this(new Scanner(System.in));
    }
    public InputHandler(Scanner scanner) {
        this.scanner = scanner;
    }
    public int getUserChoice(int min, int max) {
        while (true) {
            System.out.print("Enter choice (" + min + "-" + max + "): ");
            String input = scanner.nextLine().trim();
            try {
                int choice = Integer.parseInt(input);
                if (choice >= min && choice <= max) {
                    return choice;
                }
                System.out.println("Please enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }
    public void waitForEnter() {
        System.out.println("\nPress Enter to continue...");
        try {
            while (System.in.available() > 0) {
                System.in.read(); // 先清空残留
            }
            System.in.read();  //wait for player to press Enter
        } catch (Exception e) {
            // ignore
        }
    }
}
