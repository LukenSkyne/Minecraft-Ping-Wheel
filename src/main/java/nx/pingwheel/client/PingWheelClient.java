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
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import nx.pingwheel.client.config.ConfigHandler;
import nx.pingwheel.shared.network.PingLocationPacketS2C;
import nx.pingwheel.shared.network.UpdateChannelPacketC2S;
import org.lwjgl.glfw.GLFW;

import java.util.function.Function;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static nx.pingwheel.shared.PingWheel.MOD_ID;

@Environment(EnvType.CLIENT)
public class PingWheelClient implements ClientModInitializer {

	public static final MinecraftClient Game = MinecraftClient.getInstance();
	public static final ConfigHandler ConfigHandler = new ConfigHandler(MOD_ID + ".json");
	public static final Identifier PING_SOUND_ID = new Identifier(MOD_ID, "ping");
	public static final SoundEvent PING_SOUND_EVENT = SoundEvent.of(PING_SOUND_ID);

	private static KeyBinding kbPing;
	private static KeyBinding kbSettings;

	@Override
	public void onInitializeClient() {
		ConfigHandler.load();

		SetupKeyBindings();
		SetupClientCommands();

		Registry.register(Registries.SOUND_EVENT, PING_SOUND_ID, PING_SOUND_EVENT);

		ClientPlayNetworking.registerGlobalReceiver(PingLocationPacketS2C.ID, (a, b, packet, c) -> ClientCore.onPingLocation(packet));
		ClientPlayConnectionEvents.JOIN.register((a, b, c) -> new UpdateChannelPacketC2S(ConfigHandler.getConfig().getChannel()).send());
	}

	private void SetupKeyBindings() {
		kbPing = KeyBindingHelper.registerKeyBinding(new KeyBinding("ping-wheel.key.mark-location", InputUtil.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_5, "ping-wheel.name"));
		kbSettings = KeyBindingHelper.registerKeyBinding(new KeyBinding("ping-wheel.key.open-settings", InputUtil.Type.KEYSYM, -1, "ping-wheel.name"));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (kbPing.wasPressed()) {
				ClientCore.markLocation();
			}

			if (kbSettings.wasPressed()) {
				Game.setScreen(new PingWheelSettingsScreen());
			}
		});
	}

	private void SetupClientCommands() {
		Function<String, String> formatChannel = (channel) -> "".equals(channel) ? "§eGlobal §7(default)" : String.format("\"§6%s§r\"", channel);

		var cmdChannel = literal("channel")
				.executes((context) -> {
					var currentChannel = ConfigHandler.getConfig().getChannel();
					context.getSource().sendFeedback(Text.of(String.format("Current Ping-Wheel channel: %s", formatChannel.apply(currentChannel))));

					return 1;
				})
				.then(argument("channel_name", StringArgumentType.string()).executes((context) -> {
					var newChannel = context.getArgument("channel_name", String.class);

					ConfigHandler.getConfig().setChannel(newChannel);
					ConfigHandler.save();

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

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(cmdBase));
	}
}
