package io.github.chloeji.svtrail.util;

import java.util.Scanner;

/**
 * Reads and validates user input from a {@link Scanner}. All console prompts
 * for the game funnel through this class so that input validation lives in
 * one place.
 */
public class InputHandler {
    private final Scanner scanner;

    /**
     * Creates a handler reading from {@code System.in}.
     */
    public InputHandler() {
        this(new Scanner(System.in));
    }

    /**
     * Creates a handler reading from a caller-supplied scanner, used by tests
     * to feed scripted input.
     *
     * @param scanner the input source
     */
    public InputHandler(Scanner scanner) {
        this.scanner = scanner;
    }

    /**
     * Prompts the player for an integer in the inclusive range
     * {@code [min, max]}, re-prompting until a valid value is entered.
     *
     * @param min lower bound (inclusive)
     * @param max upper bound (inclusive)
     * @return the validated user choice
     */
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

    /**
     * Blocks until the player presses Enter. Used to pause the game so event
     * results stay on screen until the player acknowledges them.
     */
    public void waitForEnter() {
        System.out.println("\nPress Enter to continue...");
        try {
            // Drain any leftover bytes from the prior Scanner.nextLine() call
            // so the upcoming System.in.read() actually blocks on a fresh
            // keypress rather than returning a stale newline immediately.
            while (System.in.available() > 0) {
                System.in.read();
            }
            System.in.read();
        } catch (Exception e) {
            // A failed availability check or read on System.in is non-fatal:
            // skipping the pause is a minor UX degradation, not a crash.
            // Intentionally swallowed so the game keeps running.
        }
    }
}
