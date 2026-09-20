package nx.pingwheel.common.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import nx.pingwheel.common.platform.IPlatformContextService;
import nx.pingwheel.common.resource.LanguageUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static nx.pingwheel.common.CommonClient.Game;
import static nx.pingwheel.common.Global.LOGGER;
import static nx.pingwheel.common.Global.MOD_ID;
import static nx.pingwheel.common.util.InputUtils.KEY_BINDING_PING;
import static nx.pingwheel.common.util.InputUtils.KEY_BINDING_SETTINGS;

// TODO: remove this class a few versions/months down the line
// introduced with 1.12.0 on 09.10.2025
@SuppressWarnings("StringConcatenationArgumentToLogCall")
public class LegacyMigrationHandler {

	private static boolean gameOptionsSaveNeeded = false;
	private static boolean resetSettingsBinding = false;
	private static boolean notifyDeprecatedResourcePack = false;

	public static void migrateConfig(String configExtension) {
		var legacyConfigPath = IPlatformContextService.INSTANCE.resolveConfigDir("ping-wheel" + configExtension);
		var newConfigPath = IPlatformContextService.INSTANCE.resolveConfigDir(MOD_ID + configExtension);

		if (!Files.exists(legacyConfigPath)) return;

		try {
			Files.move(legacyConfigPath, newConfigPath, StandardCopyOption.REPLACE_EXISTING);
			LOGGER.info("Renamed legacy config: %s -> %s".formatted(legacyConfigPath, newConfigPath));
		} catch (IOException e) {
			LOGGER.error("Failed to rename legacy config: %s".formatted(e));
		}
	}

	public static void migrateKeyMappings() {
		var optionsPath = IPlatformContextService.INSTANCE.resolveGameDir("options.txt");

		if (!Files.exists(optionsPath)) {
			LOGGER.warn("Unable to find game options for migration routine");
			return;
		}

		try {
			var lines = Files.readAllLines(optionsPath);

			for (String line : lines) {
				if (line.contains("pingwheel.open_settings") && line.contains("key.keyboard.-1")) {
					resetSettingsBinding = true;
					gameOptionsSaveNeeded = true;
				}

				if (!line.contains("ping-wheel")) continue;

				var keyString = line.split(":")[1];
				var keyKey = InputConstants.getKey(keyString);

				if (line.contains("ping-location")) {
					KEY_BINDING_PING.setKey(keyKey);
					LOGGER.info("Migrated KEY_BINDING_PING: %s".formatted(keyKey));
				} else if (line.contains("open-settings")) {
					KEY_BINDING_SETTINGS.setKey(keyKey);
					LOGGER.info("Migrated KEY_BINDING_SETTINGS: %s".formatted(keyKey));
				}

				gameOptionsSaveNeeded = true;
			}
		} catch (IOException e) {
			LOGGER.error("Failed to read options.txt to transfer legacy keybinds: %s".formatted(e));
		}
	}

	public static void onTick() {
		if (Game == null) return;

		if (gameOptionsSaveNeeded) {
			if (resetSettingsBinding) {
				KEY_BINDING_SETTINGS.setKey(KEY_BINDING_SETTINGS.getDefaultKey());
			}

			Game.options.save();
			gameOptionsSaveNeeded = false;
		}

		if (notifyDeprecatedResourcePack && Game.player != null) {
			var msg = Component.literal("a resource pack is using the old mod-id, please update ");
			msg.append(Component.literal("\"assets/ping-wheel\"").withStyle(ChatFormatting.GRAY));
			msg.append(" to ");
			msg.append(Component.literal("\"assets/pingwheel\"").withStyle(ChatFormatting.GRAY));

			Game.player.sendSystemMessage(LanguageUtils.withModPrefix(msg));
			notifyDeprecatedResourcePack = false;
		}
	}

	public static void checkResources(ResourceManager resourceManager) {
		var legacyTextures = List.of(
			Identifier.fromNamespaceAndPath("ping-wheel", "ping"),
			Identifier.fromNamespaceAndPath("ping-wheel", "textures/ping.png"),
			Identifier.fromNamespaceAndPath("ping-wheel", "textures/arrow.png")
		);

		notifyDeprecatedResourcePack = false;

		for (var resource : legacyTextures) {
			notifyDeprecatedResourcePack |= !resourceManager.getResourceStack(resource).isEmpty();
		}
	}
}
