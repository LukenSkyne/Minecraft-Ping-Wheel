package nx.pingwheel.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static nx.pingwheel.common.Global.LOGGER;

public class ConfigHandler <T extends IConfig> {

	private final Gson gson;
	private final Class<T> configType;
	private final Path configPath;

	@Getter
	private T config;
	private int configHash;

	@SneakyThrows
	public ConfigHandler(Class<T> configType, Path configPath) {
		this.gson = new GsonBuilder().setPrettyPrinting().create();
		this.configType = configType;
		this.configPath = configPath;

		this.configHash = 0;
		this.config = configType.getDeclaredConstructor().newInstance();
	}

	public void save() {
		if (configHash == config.hashCode()) {
			return;
		}

		config.onUpdate();

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

	@SneakyThrows
	public void load() {
		if (!Files.exists(configPath)) {
			save();
			return;
		}

		try {
			var reader = Files.newBufferedReader(configPath);
			config = gson.fromJson(reader, configType);
			reader.close();
		} catch (Exception e) {
			config = null;
		}

		if (config == null) {
			config = configType.getDeclaredConstructor().newInstance();
			LOGGER.error("Config is broken -> reset to defaults");

			save();
			return;
		}

		config.validate();
		configHash = config.hashCode();
		LOGGER.info("Loaded " + config);
	}
}
