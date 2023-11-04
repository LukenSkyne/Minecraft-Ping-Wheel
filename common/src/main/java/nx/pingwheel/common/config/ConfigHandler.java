package nx.pingwheel.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import nx.pingwheel.common.networking.UpdateChannelPacketC2S;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static nx.pingwheel.common.Global.LOGGER;

public class ConfigHandler {

	private final Gson gson;
	private final Path configPath;

	@Getter
	private Config config;
	private int configHash;

	public ConfigHandler(String configName, Path configDir) {
		this.gson = new GsonBuilder().setPrettyPrinting().create();
		this.configPath = configDir.resolve(configName);

		this.configHash = 0;
		this.config = new Config();
	}

	public void save() {
		if (configHash == config.hashCode()) {
			return;
		}

		new UpdateChannelPacketC2S(config.getChannel()).send();

		if (!Files.exists(configPath)) {
			try {
				Files.createDirectories(configPath.getParent());
				Files.createFile(configPath);
			} catch (IOException e) {
				LOGGER.error("Creating Config failed: " + e);
				return;
			}
		}

		try {
			var writer = Files.newBufferedWriter(configPath);
			gson.toJson(config, writer);
			writer.close();
		} catch (Exception e) {
			LOGGER.error("Saving Config failed: " + e);
			return;
		}

		configHash = config.hashCode();
		LOGGER.info("Saved " + config);
	}

	public void load() {
		if (!Files.exists(configPath)) {
			save();
			return;
		}

		try {
			var reader = Files.newBufferedReader(configPath);
			config = gson.fromJson(reader, Config.class);
			reader.close();
		} catch (Exception e) {
			config = null;
		}

		if (config == null) {
			config = new Config();
			LOGGER.error("Config is broken -> reset to defaults");

			save();
			return;
		}

		configHash = config.hashCode();
		LOGGER.info("Loaded " + config);
	}
}
