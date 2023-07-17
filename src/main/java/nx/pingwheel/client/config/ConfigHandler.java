package nx.pingwheel.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import nx.pingwheel.shared.network.UpdateChannelPacketC2S;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static nx.pingwheel.shared.PingWheel.Game;
import static nx.pingwheel.shared.PingWheel.LOGGER;

public class ConfigHandler {

	final private Gson gson;
	final private Path configPath;
	final private String configName;

	private int configHash;
	private Config config;

	public ConfigHandler(String configName) {
		this.gson = new GsonBuilder().setPrettyPrinting().create();
		this.configPath = FabricLoader.getInstance().getConfigDir();
		this.configName = configName;

		this.configHash = 0;
		this.config = new Config();
	}

	public Config getConfig() {
		return config;
	}

	public void save() {
		if (configHash == config.hashCode()) {
			return;
		}

		if (Game.getNetworkHandler() != null) {
			new UpdateChannelPacketC2S(config.getChannel()).send();
		}

		var configFile = configPath.resolve(configName).toFile();

		if (!configFile.exists()) {
			configFile.getParentFile().mkdirs();

			try {
				Files.createFile(configFile.toPath());
			} catch (IOException e) {
				LOGGER.error("Creating Config failed: " + e);
				return;
			}
		}

		try {
			var writer = Files.newBufferedWriter(configFile.toPath());
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
		var configFile = configPath.resolve(configName).toFile();

		if (!configFile.exists()) {
			save();
			return;
		}

		try {
			var reader = Files.newBufferedReader(configFile.toPath());
			config = gson.fromJson(reader, Config.class);
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
