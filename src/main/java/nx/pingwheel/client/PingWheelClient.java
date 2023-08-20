package nx.pingwheel.client;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.util.InputUtil;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.resource.ResourceType;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.Registry;
import nx.pingwheel.client.config.ConfigHandler;
import nx.pingwheel.shared.network.PingLocationPacketS2C;
import nx.pingwheel.shared.network.UpdateChannelPacketC2S;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

import static net.fabricmc.fabric.api.client.command.v1.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v1.ClientCommandManager.literal;
import static nx.pingwheel.shared.PingWheel.MOD_ID;

@Environment(EnvType.CLIENT)
public class PingWheelClient implements ClientModInitializer {

	public static final MinecraftClient Game = MinecraftClient.getInstance();
	public static final ConfigHandler ConfigHandler = new ConfigHandler(MOD_ID + ".json");
	public static final Identifier RELOAD_LISTENER_ID = new Identifier(MOD_ID, "reload-listener");
	public static final Identifier PING_SOUND_ID = new Identifier(MOD_ID, "ping");
	public static final SoundEvent PING_SOUND_EVENT = new SoundEvent(PING_SOUND_ID);
	public static final Identifier PING_TEXTURE_ID = new Identifier(MOD_ID, "textures/ping.png");

	private static KeyBinding kbPing;
	private static KeyBinding kbSettings;

	@Override
	public void onInitializeClient() {
		ConfigHandler.load();

		SetupKeyBindings();
		SetupClientCommands();

		Registry.register(Registry.SOUND_EVENT, PING_SOUND_ID, PING_SOUND_EVENT);

		ClientPlayNetworking.registerGlobalReceiver(PingLocationPacketS2C.ID, (a, b, packet, c) -> ClientCore.onPingLocation(packet));
		ClientPlayConnectionEvents.JOIN.register((a, b, c) -> new UpdateChannelPacketC2S(ConfigHandler.getConfig().getChannel()).send());

		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES)
			.registerReloadListener(new IdentifiableResourceReloadListener() {
				@Override
				public Identifier getFabricId() {
					return RELOAD_LISTENER_ID;
				}

				@Override
				public CompletableFuture<Void> reload(Synchronizer helper, ResourceManager resourceManager, Profiler loadProfiler, Profiler applyProfiler, Executor loadExecutor, Executor applyExecutor) {
					return reloadTextures(helper, resourceManager, loadExecutor, applyExecutor);
				}
			});
	}

	private CompletableFuture<Void> reloadTextures(ResourceReloader.Synchronizer helper, ResourceManager resourceManager, Executor loadExecutor, Executor applyExecutor) {
		return CompletableFuture
			.supplyAsync(() -> {
				final var canLoadTexture = resourceManager.containsResource(PING_TEXTURE_ID);

				if (!canLoadTexture) {
					// force texture manager to remove the entry from its index
					Game.getTextureManager().registerTexture(PING_TEXTURE_ID, MissingSprite.getMissingSpriteTexture());
				}

				return canLoadTexture;
			}, loadExecutor)
			.thenCompose(helper::whenPrepared)
			.thenAcceptAsync(canLoadTexture -> {
				if (canLoadTexture) {
					Game.getTextureManager().bindTexture(PING_TEXTURE_ID);
				}
			}, applyExecutor);
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
					context.getSource().sendFeedback(new LiteralText(String.format("Current Ping-Wheel channel: %s", formatChannel.apply(currentChannel))));

					return 1;
				})
				.then(argument("channel_name", StringArgumentType.string()).executes((context) -> {
					var newChannel = context.getArgument("channel_name", String.class);

					ConfigHandler.getConfig().setChannel(newChannel);
					ConfigHandler.save();

					context.getSource().sendFeedback(new LiteralText(String.format("Set Ping-Wheel channel to: %s", formatChannel.apply(newChannel))));

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

			context.getSource().sendFeedback(new LiteralText(output));

			return 1;
		};

		var cmdHelp = literal("help")
				.executes(helpCallback);

		var cmdBase = literal("pingwheel")
				.executes(helpCallback)
				.then(cmdHelp)
				.then(cmdConfig)
				.then(cmdChannel);

		ClientCommandManager.DISPATCHER.register(cmdBase);
	}
}
