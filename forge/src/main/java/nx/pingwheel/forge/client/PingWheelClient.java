package nx.pingwheel.forge.client;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import nx.pingwheel.forge.client.config.ConfigHandler;
import nx.pingwheel.forge.shared.network.UpdateChannelPacketC2S;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;
import java.util.function.UnaryOperator;

import static nx.pingwheel.forge.shared.PingWheel.MOD_ID;
import static nx.pingwheel.forge.shared.PingWheel.PING_LOCATION_CHANNEL_S2C;

@OnlyIn(Dist.CLIENT)
public class PingWheelClient {

	public static final MinecraftClient Game = MinecraftClient.getInstance();
	public static final ConfigHandler ConfigHandler = new ConfigHandler(MOD_ID + ".json");
	public static final Identifier PING_SOUND_ID = new Identifier(MOD_ID, "ping");
	public static final SoundEvent PING_SOUND_EVENT = new SoundEvent(PING_SOUND_ID);
	public static final Identifier PING_TEXTURE_ID = new Identifier(MOD_ID, "textures/ping.png");

	public static final KeyBinding kbPing = new KeyBinding("ping-wheel.key.mark-location", InputUtil.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_5, "ping-wheel.name");
	public static final KeyBinding kbSettings = new KeyBinding("ping-wheel.key.open-settings", InputUtil.Type.KEYSYM, -1, "ping-wheel.name");

	public PingWheelClient() {
		ConfigHandler.load();

		MinecraftForge.EVENT_BUS.register(this);
		var bus = FMLJavaModLoadingContext.get().getModEventBus();

		ClientRegistry.registerKeyBinding(kbPing);
		ClientRegistry.registerKeyBinding(kbSettings);

		//Registry.register(Registry.SOUND_EVENT, PING_SOUND_ID, PING_SOUND_EVENT);

		PING_LOCATION_CHANNEL_S2C.addListener((event) -> {
			var ctx = event.getSource().get();
			var packet = event.getPayload();

			if (packet != null) {
				ctx.enqueueWork(() -> ClientCore.onPingLocation(packet));
			}

			ctx.setPacketHandled(true);
		});

		bus.addListener((RegisterClientReloadListenersEvent event) -> event.registerReloadListener(new ResourceReloadListener()));
	}

	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase.equals(TickEvent.Phase.END)) {
			if (kbPing.wasPressed()) {
				ClientCore.markLocation();
			}

			if (kbSettings.wasPressed()) {
				Game.setScreen(new PingWheelSettingsScreen());
			}
		}
	}

	@SubscribeEvent
	public void onClientConnectedToServer(ClientPlayerNetworkEvent.LoggedInEvent event) {
		new UpdateChannelPacketC2S(ConfigHandler.getConfig().getChannel()).send();
	}

	@SubscribeEvent
	public void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage().equals(RenderLevelStageEvent.Stage.AFTER_WEATHER)) {
			ClientCore.onRenderWorld(event.getPoseStack(), event.getProjectionMatrix(), event.getPartialTick());
		}
	}

	@SubscribeEvent
	public void onPreGuiRender(RenderGameOverlayEvent.Pre event) {
		if (Objects.equals(event.getType(), RenderGameOverlayEvent.ElementType.ALL)) {
			ClientCore.onRenderGUI(event.getMatrixStack(), event.getPartialTicks());
		}
	}

	@SubscribeEvent
	public void onCommandRegister(RegisterClientCommandsEvent event) {
		UnaryOperator<String> formatChannel = (channel) -> "".equals(channel) ? "§eGlobal §7(default)" : String.format("\"§6%s§r\"", channel);

		var cmdChannel = LiteralArgumentBuilder.<ServerCommandSource>literal("channel")
			.executes((context) -> {
				var currentChannel = ConfigHandler.getConfig().getChannel();
				context.getSource().sendFeedback(Text.of(String.format("Current Ping-Wheel channel: %s", formatChannel.apply(currentChannel))), false);

				return 1;
			})
			.then(RequiredArgumentBuilder.<ServerCommandSource, String>argument("channel_name", StringArgumentType.string()).executes((context) -> {
				var newChannel = context.getArgument("channel_name", String.class);

				ConfigHandler.getConfig().setChannel(newChannel);
				ConfigHandler.save();

				context.getSource().sendFeedback(Text.of(String.format("Set Ping-Wheel channel to: %s", formatChannel.apply(newChannel))), false);

				return 1;
			}));

		var cmdConfig = LiteralArgumentBuilder.<ServerCommandSource>literal("config")
			.executes((context) -> {
				Game.send(() -> Game.setScreen(new PingWheelSettingsScreen()));
				return 1;
			});

		Command<ServerCommandSource> helpCallback = (context) -> {
			var output = """
				§f/pingwheel config
				§7(manage pingwheel configuration)
				§f/pingwheel channel
				§7(get your current channel)
				§f/pingwheel channel <channel_name>
				§7(set your current channel, use "" for global channel)""";

			context.getSource().sendFeedback(Text.of(output), false);

			return 1;
		};

		var cmdHelp = LiteralArgumentBuilder.<ServerCommandSource>literal("help")
			.executes(helpCallback);

		var cmdBase = LiteralArgumentBuilder.<ServerCommandSource>literal("pingwheel")
			.executes((context) -> {
				var output = """
				§f/pingwheel config
				§7(manage pingwheel configuration)
				§f/pingwheel channel
				§7(get your current channel)
				§f/pingwheel channel <channel_name>
				§7(set your current channel, use "" for global channel)""";

				context.getSource().sendFeedback(Text.of(output), false);

				return 1;
			})
			.then(cmdHelp)
			.then(cmdConfig)
			.then(cmdChannel);

		event.getDispatcher().register(cmdBase);
	}
}
