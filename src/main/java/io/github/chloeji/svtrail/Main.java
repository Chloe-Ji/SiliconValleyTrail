package io.github.chloeji.svtrail;

import io.github.chloeji.svtrail.core.GameRunner;

/**
 * Application entry point. Constructs a {@link GameRunner} and hands off
 * control to its main menu loop.
 */
public class Main {

    /**
     * JVM entry point.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        GameRunner game = new GameRunner();
        game.start();
    }
}
