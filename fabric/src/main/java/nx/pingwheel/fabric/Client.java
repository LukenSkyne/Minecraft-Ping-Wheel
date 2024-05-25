package nx.pingwheel.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import nx.pingwheel.common.commands.ClientCommandBuilder;
import nx.pingwheel.common.config.ClientConfig;
import nx.pingwheel.common.config.ConfigHandler;
import nx.pingwheel.common.core.ClientCore;
import nx.pingwheel.common.networking.PingLocationS2CPacket;
import nx.pingwheel.common.networking.UpdateChannelC2SPacket;
import nx.pingwheel.common.resource.ResourceReloadListener;
import nx.pingwheel.common.screen.SettingsScreen;
import nx.pingwheel.fabric.event.GuiRenderCallback;
import nx.pingwheel.fabric.event.WorldRenderCallback;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static nx.pingwheel.common.ClientGlobal.*;
import static nx.pingwheel.common.Global.MOD_ID;
import static nx.pingwheel.common.Global.NetHandler;

@Environment(EnvType.CLIENT)
public class Client implements ClientModInitializer {

	public static final ResourceLocation RELOAD_LISTENER_ID = new ResourceLocation(MOD_ID, "reload-listener");

	@Override
	public void onInitializeClient() {
		ConfigHandler = new ConfigHandler<>(ClientConfig.class, FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json"));
		ConfigHandler.load();

		Registry.register(Registry.SOUND_EVENT, PING_SOUND_ID, PING_SOUND_EVENT);

		registerNetworkPackets();
		registerReloadListener();
		registerKeyBindings();

		// register client connection callback
		ClientPlayConnectionEvents.JOIN.register((a, b, c) -> NetHandler.sendToServer(new UpdateChannelC2SPacket(ConfigHandler.getConfig().getChannel())));

		// register rendering callbacks
		GuiRenderCallback.START.register(ClientCore::onRenderGUI);
		WorldRenderCallback.START.register(ClientCore::onRenderWorld);

		// register commands
		ClientCommandManager.DISPATCHER.register(ClientCommandBuilder.build((context, success, response) -> {
			if (success) {
				context.getSource().sendFeedback(response);
			} else {
				context.getSource().sendError(response);
			}
		}));
	}

	private void registerNetworkPackets() {
		ClientPlayNetworking.registerGlobalReceiver(PingLocationS2CPacket.PACKET_ID, (a, b, packet, c) -> ClientCore.onPingLocation(PingLocationS2CPacket.readSafe(packet)));
	}

	private void registerReloadListener() {
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
			.registerReloadListener(new IdentifiableResourceReloadListener() {
				@Override
				public ResourceLocation getFabricId() {
					return RELOAD_LISTENER_ID;
				}

				@Override
				public CompletableFuture<Void> reload(PreparationBarrier helper, ResourceManager resourceManager, ProfilerFiller loadProfiler, ProfilerFiller applyProfiler, Executor loadExecutor, Executor applyExecutor) {
					return ResourceReloadListener.reloadTextures(helper, resourceManager, loadExecutor, applyExecutor);
				}
			});
	}

	private void registerKeyBindings() {
		KeyBindingHelper.registerKeyBinding(KEY_BINDING_PING);
		KeyBindingHelper.registerKeyBinding(KEY_BINDING_SETTINGS);
		KeyBindingHelper.registerKeyBinding(KEY_BINDING_NAME_LABELS);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (KEY_BINDING_PING.consumeClick()) {
				ClientCore.pingLocation();
			}

			if (KEY_BINDING_SETTINGS.consumeClick()) {
				Game.setScreen(new SettingsScreen());
			}
		});
	}
}
