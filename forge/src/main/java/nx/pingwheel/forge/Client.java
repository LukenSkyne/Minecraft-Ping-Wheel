package nx.pingwheel.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import nx.pingwheel.common.config.ConfigHandler;
import nx.pingwheel.common.core.ClientCore;
import nx.pingwheel.common.helper.ClientCommandBuilder;
import nx.pingwheel.common.networking.UpdateChannelPacketC2S;
import nx.pingwheel.common.resource.ResourceReloadListener;
import nx.pingwheel.common.screen.SettingsScreen;

import java.util.Objects;

import static nx.pingwheel.common.ClientGlobal.*;
import static nx.pingwheel.common.Global.MOD_ID;
import static nx.pingwheel.forge.Main.PING_LOCATION_CHANNEL_S2C;

@OnlyIn(Dist.CLIENT)
public class Client {

	public Client() {
		ConfigHandler = new ConfigHandler(MOD_ID + ".json", FMLPaths.CONFIGDIR.get());
		ConfigHandler.load();

		MinecraftForge.EVENT_BUS.register(this);

		registerNetworkPackets();
		registerReloadListener();
		registerKeyBindings();
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

	private void registerKeyBindings() {
		ClientRegistry.registerKeyBinding(KEY_BINDING_PING);
		ClientRegistry.registerKeyBinding(KEY_BINDING_SETTINGS);
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
	public void onClientConnectedToServer(ClientPlayerNetworkEvent.LoggedInEvent event) {
		new UpdateChannelPacketC2S(ConfigHandler.getConfig().getChannel()).send();
	}

	@SubscribeEvent
	public void onRenderWorld(RenderLevelStageEvent event) {
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
		event.getDispatcher().register(ClientCommandBuilder.build((context, response) -> context.getSource().sendFeedback(response, false)));
	}
}
