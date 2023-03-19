package nx.pingwheel.client;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import nx.pingwheel.shared.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.util.function.Function;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

@Environment(EnvType.CLIENT)
public class PingWheelClient implements ClientModInitializer {

	public static final Logger LOGGER = LogManager.getLogger("nx-ping-wheel");

	private static KeyBinding kbPing;
	private static KeyBinding kbSettings;

	@Override
	public void onInitializeClient() {
		LOGGER.info("[Ping-Wheel] Client Init");

		PingWheelConfigHandler.getInstance().load();

		SetupKeyBindings();
		SetupClientCommands();

		ClientPlayNetworking.registerGlobalReceiver(Constants.S2C_PING_LOCATION, Core::onReceivePing);
	}

	private void SetupKeyBindings() {
		kbPing = KeyBindingHelper.registerKeyBinding(new KeyBinding("ping-wheel.key.mark-location", InputUtil.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_5, "ping-wheel.name"));
		kbSettings = KeyBindingHelper.registerKeyBinding(new KeyBinding("ping-wheel.key.open-settings", InputUtil.Type.KEYSYM, -1, "ping-wheel.name"));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (kbPing.wasPressed()) {
				Core.markLocation();
			}

			if (kbSettings.wasPressed()) {
				MinecraftClient.getInstance().setScreen(new PingWheelSettingsScreen());
			}
		});
	}

	private void SetupClientCommands() {
		Function<String, String> formatChannel = (channel) -> "".equals(channel) ? "§eGlobal §7(default)" : String.format("\"§6%s§r\"", channel);

		var cmdChannel = literal("channel")
				.executes((context) -> {
					var currentChannel = PingWheelConfigHandler.getInstance().getConfig().getChannel();
					context.getSource().sendFeedback(Text.of(String.format("Current Ping-Wheel channel: %s", formatChannel.apply(currentChannel))));

					return 1;
				})
				.then(argument("channel_name", StringArgumentType.string()).executes((context) -> {
					var newChannel = context.getArgument("channel_name", String.class);

					PingWheelConfigHandler.getInstance().getConfig().setChannel(newChannel);
					PingWheelConfigHandler.getInstance().save();

					context.getSource().sendFeedback(Text.of(String.format("Set Ping-Wheel channel to: %s", formatChannel.apply(newChannel))));

					return 1;
				}));

		var cmdConfig = literal("config")
				.executes((context) -> {
					var client = context.getSource().getClient();
					client.send(() -> client.setScreen(new PingWheelSettingsScreen()));

					return 1;
				});

		Command<FabricClientCommandSource> helpCallback = (context) -> {
			var output = """
				§f/pingwheel config
				§7(manage pingwheel configuration)
				§f/pingwheel channel
				§7(get your current channel)
				§f/pingwheel channel <channel_name>
				§7(set your current channel, use "" for global channel)""";

			context.getSource().sendFeedback(Text.of(output));

			return 1;
		};

		var cmdHelp = literal("help")
				.executes(helpCallback);

		var cmdBase = literal("pingwheel")
				.executes(helpCallback)
				.then(cmdHelp)
				.then(cmdConfig)
				.then(cmdChannel);

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(cmdBase);
		});
	}
}
