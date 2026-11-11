package net.vsh33.notextwall.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.vsh33.notextwall.NoTextWall;

/**
 * Settings stored in {@code config/notextwall.json} (same file name as the original 1.20.4 mod, so old files keep working).
 * Every message is checked against the live instance, so changes from the command, the Mod Menu screen or
 * {@code /notextwall reload} apply immediately - no restart needed.
 * Missing fields (e.g. after updating the mod) are filled with defaults and written back to the file on load.
 */
public final class NoTextWallConfig {
	public static final int MIN_LENGTH = 1;
	public static final int MAX_LENGTH = 32767;
	public static final int MAX_LINES_LIMIT = 100;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Path file;
	private static NoTextWallConfig instance = new NoTextWallConfig();

	/** Master switch. */
	public boolean enabled = true;
	/** Messages longer than this many characters are hidden (added in 0.1). */
	public int maxMessageLength = 100;
	/** Messages with more than this many lines are hidden as well. 0 = no line limit. */
	public int maxLines = 0;
	/** Show "[NoTextWall] Long message hidden" in place of the hidden message (added in 0.2). */
	public boolean showCancelMessage = true;
	/** Hovering the notice shows the beginning of the hidden message; clicking it prints the whole message once. */
	public boolean showPreview = true;
	/** Check signed player chat messages. */
	public boolean filterPlayerChat = true;
	/** Check system / server messages (Discord bridges, plugins, /say, /tellraw, ...). */
	public boolean filterSystemMessages = true;

	public static NoTextWallConfig get() {
		return instance;
	}

	public static Path file() {
		if (file == null) {
			file = FabricLoader.getInstance().getConfigDir().resolve("notextwall.json");
		}
		return file;
	}

	/** Loads (or creates) the config file and rewrites it so every current option is present. */
	public static void load() {
		NoTextWallConfig loaded = null;
		Path path = file();
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				loaded = GSON.fromJson(reader, NoTextWallConfig.class);
			} catch (Exception e) {
				NoTextWall.LOGGER.error("Could not read {}, using defaults", path, e);
			}
		}
		instance = loaded != null ? loaded : new NoTextWallConfig();
		instance.validate();
		save();
	}

	public static void save() {
		Path path = file();
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			NoTextWall.LOGGER.error("Could not write {}", path, e);
		}
	}

	public static void reset() {
		instance = new NoTextWallConfig();
		save();
	}

	/** Clamps numbers into sane ranges (hand-edited files may contain anything). */
	public void validate() {
		maxMessageLength = Math.clamp(maxMessageLength, MIN_LENGTH, MAX_LENGTH);
		maxLines = Math.clamp(maxLines, 0, MAX_LINES_LIMIT);
	}
}
