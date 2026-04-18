package org.example.util;
import com.google.gson.Gson;
import org.example.model.StartupState;
import java.io.*;

public class SaveManager {
    private static final String SAVE_FILE = "save.json";
    private final Gson gson;

    public SaveManager() {
        this.gson = new Gson();
    }
    public void save(StartupState state) {
        try (Writer writer = new FileWriter(SAVE_FILE)) {
            gson.toJson(state, writer);
            System.out.println("💾 Game saved!");
        } catch (IOException e) {
            System.out.println("⚠️ Failed to save game.");
        }
    }
    public StartupState load() {
        try (Reader reader = new FileReader(SAVE_FILE)) {
            StartupState state = gson.fromJson(reader, StartupState.class);
            System.out.println("✅ Game loaded successfully!");
            return state;
        } catch (IOException e) {
            System.out.println("⚠️ No save file found.");
            return null;
        }
    }
    public boolean hasSaveFile() {
        return new File(SAVE_FILE).exists();
    }
}
