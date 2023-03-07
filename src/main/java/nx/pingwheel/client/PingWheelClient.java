package nx.pingwheel.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.LiteralText;
import nx.pingwheel.shared.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.util.function.Function;

import static net.fabricmc.fabric.api.client.command.v1.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v1.ClientCommandManager.literal;

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
		kbSettings = KeyBindingHelper.registerKeyBinding(new KeyBinding("ping-wheel.key.open-settings", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F6, "ping-wheel.name"));

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
					context.getSource().sendFeedback(new LiteralText(String.format("Current Ping-Wheel channel: %s", formatChannel.apply(currentChannel))));

					return 1;
				})
				.then(argument("channel_name", StringArgumentType.string()).executes((context) -> {
					var newChannel = context.getArgument("channel_name", String.class);

					PingWheelConfigHandler.getInstance().getConfig().setChannel(newChannel);
					PingWheelConfigHandler.getInstance().save();

					context.getSource().sendFeedback(new LiteralText(String.format("Set Ping-Wheel channel to: %s", formatChannel.apply(newChannel))));

					return 1;
				}));

		var cmd = literal("pingwheel")
				.executes((context) -> {
					context.getSource().sendFeedback(new LiteralText("/pingwheel channel [<channel_name>]"));

					return 1;
				})
				.then(cmdChannel);

		ClientCommandManager.DISPATCHER.register(cmd);
	}
}
