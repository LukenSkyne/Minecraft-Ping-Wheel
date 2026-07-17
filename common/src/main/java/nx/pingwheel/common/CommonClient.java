package nx.pingwheel.common;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import nx.pingwheel.common.compat.LegacyMigrationHandler;
import nx.pingwheel.common.config.ClientConfig;
import nx.pingwheel.common.core.GameContext;
import nx.pingwheel.common.core.PingController;
import nx.pingwheel.common.core.PingManager;
import nx.pingwheel.common.network.PingLocationS2CPacket;
import nx.pingwheel.common.network.UpdateChannelC2SPacket;
import nx.pingwheel.common.platform.IPlatformClientEventService;
import nx.pingwheel.common.platform.IPlatformContextService;
import nx.pingwheel.common.platform.IPlatformNetworkService;
import nx.pingwheel.common.render.OverlayRenderer;
import nx.pingwheel.common.render.WorldRenderContext;
import nx.pingwheel.common.screen.SettingsScreen;
import nx.pingwheel.common.util.InputUtils;

import static nx.pingwheel.common.util.InputUtils.KEY_BINDING_PING;
import static nx.pingwheel.common.util.InputUtils.KEY_BINDING_SETTINGS;

public class CommonClient {

	public static final CommonClient INSTANCE = new CommonClient();
	public static Minecraft Game = null;

	private CommonClient() {}

	public void onInit() {
		ClientConfig.HANDLER.load();

		IPlatformClientEventService.INSTANCE.registerTickStartEvent(this::onTickStart);
		IPlatformClientEventService.INSTANCE.registerJoinServerEvent(this::onJoinServer);
		IPlatformClientEventService.INSTANCE.registerLeaveServerEvent(this::onLeaveServer);

		IPlatformContextService.INSTANCE.registerKeyMapping(KEY_BINDING_PING);
		IPlatformContextService.INSTANCE.registerKeyMapping(KEY_BINDING_SETTINGS);

		LegacyMigrationHandler.migrateKeyMappings();
	}

	public void onJoinServer() {
		IPlatformNetworkService.INSTANCE.sendToServer(new UpdateChannelC2SPacket(ClientConfig.HANDLER.getConfig().getChannel()));
	}

	public void onLeaveServer() {
		PingManager.clearPings();
	}

	public void onTickStart() {
		Game = Minecraft.getInstance();
		GameContext.updateDimension();

		LegacyMigrationHandler.onTick();

		if (InputUtils.consumePingHotkey()) {
			PingController.queuePingAction();
		}

		if (KEY_BINDING_SETTINGS.consumeClick()) {
			Game.gui.setScreen(new SettingsScreen());
		}
	}

	public void onRenderWorld(WorldRenderContext ctx) {
		PingManager.updatePings(ctx);
		PingController.pollPingAction(ctx.tickDelta);
	}

	public void onRenderGUI(GuiGraphicsExtractor guiGraphics, float tickDelta) {
		OverlayRenderer.draw(guiGraphics, tickDelta);
	}

	public void onPingLocationPacket(PingLocationS2CPacket packet) {
		PingManager.acceptPingPacket(packet);
	}
}
