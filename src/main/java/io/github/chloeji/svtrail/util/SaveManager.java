package io.github.chloeji.svtrail.util;

import com.google.gson.Gson;
import io.github.chloeji.svtrail.model.StartupState;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * Serializes and deserializes {@link StartupState} to a single JSON save file
 * in the working directory. A single slot is sufficient for a single-player
 * CLI game; multi-slot support would only require parameterizing the filename.
 */
public class SaveManager {
    private static final String SAVE_FILE = "save.json";
    private final Gson gson;

    /**
     * Creates a save manager backed by a default {@link Gson} instance.
     */
    public SaveManager() {
        this.gson = new Gson();
    }

    /**
     * Serializes the given state to {@link #SAVE_FILE} and prints a
     * confirmation banner. Used by the menu's explicit Save option.
     *
     * @param state the state to persist
     */
    public void save(StartupState state) {
        if (writeToDisk(state)) {
            System.out.println("💾 Game saved!");
        } else {
            System.out.println("⚠️ Failed to save game.");
        }
    }

    /**
     * Serializes the given state to {@link #SAVE_FILE} without any console
     * output. Used as a silent auto-save after every completed turn so the
     * player never loses progress to a Ctrl+C, terminal close, or kernel
     * panic — the manual {@link #save} still exists for explicit confirmation.
     *
     * @param state the state to persist
     */
    public void saveQuietly(StartupState state) {
        writeToDisk(state);
    }

    /**
     * Writes {@code state} to the save file. Returns {@code true} on success.
     * Degrades gracefully on I/O failure: the session stays playable even if
     * the disk is read-only or full, and the exception is intentionally not
     * propagated — the caller decides whether to inform the player.
     */
    private boolean writeToDisk(StartupState state) {
        try (Writer writer = new FileWriter(SAVE_FILE)) {
            gson.toJson(state, writer);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Loads a previously saved state from {@link #SAVE_FILE}.
     *
     * @return the deserialized state, or {@code null} if no save exists or
     *         the file could not be read
     */
    public StartupState load() {
        try (Reader reader = new FileReader(SAVE_FILE)) {
            StartupState state = gson.fromJson(reader, StartupState.class);
            System.out.println("✅ Game loaded successfully!");
            return state;
        } catch (IOException e) {
            // Missing or unreadable save is expected on first run; returning
            // null lets the caller (GameRunner) stay on the main menu.
            System.out.println("⚠️ No save file found.");
            return null;
        }
    }

    /**
     * @return {@code true} if a save file currently exists on disk
     */
    public boolean hasSaveFile() {
        return new File(SAVE_FILE).exists();
    }
}
