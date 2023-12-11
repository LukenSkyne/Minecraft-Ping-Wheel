package nx.pingwheel.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import nx.pingwheel.common.config.ClientConfig;
import nx.pingwheel.common.config.ConfigHandler;
import nx.pingwheel.common.core.ClientCore;
import nx.pingwheel.common.helper.ClientCommandBuilder;
import nx.pingwheel.common.networking.UpdateChannelPacketC2S;
import nx.pingwheel.common.resource.ResourceReloadListener;
import nx.pingwheel.common.screen.SettingsScreen;

import static nx.pingwheel.common.ClientGlobal.*;
import static nx.pingwheel.common.Global.MOD_ID;
import static nx.pingwheel.forge.Main.PING_LOCATION_CHANNEL_S2C;

@OnlyIn(Dist.CLIENT)
public class Client {

	public Client() {
		ConfigHandler = new ConfigHandler<>(ClientConfig.class, FMLPaths.CONFIGDIR.get().resolve(MOD_ID + ".json"));
		ConfigHandler.load();

		MinecraftForge.EVENT_BUS.register(this);

		registerNetworkPackets();
		registerReloadListener();

		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onRegisterKeyBindings);

		ModLoadingContext.get().registerExtensionPoint(
			ConfigScreenFactory.class,
			() -> new ConfigScreenFactory((client, parent) -> new SettingsScreen(parent))
		);
	}

	private void registerNetworkPackets() {
		PING_LOCATION_CHANNEL_S2C.addListener((event) -> {
			var ctx = event.getSource().get();
			var packet = event.getPayload();

			if (packet != null) {
				ctx.enqueueWork(() -> ClientCore.onPingLocation(packet));
			}

			ctx.setPacketHandled(true);
		});
	}

	private void registerReloadListener() {
		var bus = FMLJavaModLoadingContext.get().getModEventBus();
		bus.addListener((RegisterClientReloadListenersEvent event) -> event.registerReloadListener(new ResourceReloadListener()));
	}

	private void onRegisterKeyBindings(RegisterKeyMappingsEvent event) {
		event.register(KEY_BINDING_PING);
		event.register(KEY_BINDING_SETTINGS);
		event.register(KEY_BINDING_NAME_LABELS);
	}

	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase.equals(TickEvent.Phase.END)) {
			if (KEY_BINDING_PING.wasPressed()) {
				ClientCore.markLocation();
			}

			if (KEY_BINDING_SETTINGS.wasPressed()) {
				Game.setScreen(new SettingsScreen());
			}
		}
	}

	@SubscribeEvent
	public void onClientConnectedToServer(ClientPlayerNetworkEvent.LoggingIn event) {
		new UpdateChannelPacketC2S(ConfigHandler.getConfig().getChannel()).send();
	}

	@SubscribeEvent
	public void onRenderWorld(RenderLevelStageEvent event) {
		if (event.getStage().equals(RenderLevelStageEvent.Stage.AFTER_WEATHER)) {
			ClientCore.onRenderWorld(event.getPoseStack(), event.getProjectionMatrix(), event.getPartialTick());
		}
	}

	@SubscribeEvent
	public void onPreGuiRender(RenderGuiOverlayEvent.Pre event) {
		if (event.getOverlay() == VanillaGuiOverlay.VIGNETTE.type()) {
			ClientCore.onRenderGUI(event.getPoseStack(), event.getPartialTick());
		}
	}

	@SubscribeEvent
	public void onCommandRegister(RegisterClientCommandsEvent event) {
		event.getDispatcher().register(ClientCommandBuilder.build((context, success, response) -> {
			if (success) {
				context.getSource().sendFeedback(response, false);
			} else {
				context.getSource().sendError(response);
			}
		}));
	}
}
