package nx.pingwheel.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import nx.pingwheel.common.config.ClientConfig;
import nx.pingwheel.common.config.ConfigHandler;
import nx.pingwheel.common.core.ClientCore;
import nx.pingwheel.common.helper.ClientCommandBuilder;
import nx.pingwheel.common.networking.PingLocationPacketS2C;
import nx.pingwheel.common.networking.UpdateChannelPacketC2S;
import nx.pingwheel.common.resource.ResourceReloadListener;
import nx.pingwheel.common.screen.SettingsScreen;
import nx.pingwheel.fabric.event.GameOverlayRenderCallback;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static nx.pingwheel.common.ClientGlobal.*;
import static nx.pingwheel.common.Global.MOD_ID;

@Environment(EnvType.CLIENT)
public class Client implements ClientModInitializer {

	public static final Identifier RELOAD_LISTENER_ID = new Identifier(MOD_ID, "reload-listener");

	@Override
	public void onInitializeClient() {
		ConfigHandler = new ConfigHandler<>(ClientConfig.class, FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json"));
		ConfigHandler.load();

		Registry.register(Registries.SOUND_EVENT, PING_SOUND_ID, PING_SOUND_EVENT);

		registerNetworkPackets();
		registerReloadListener();
		registerKeyBindings();

		// register client connection callback
		ClientPlayConnectionEvents.JOIN.register((a, b, c) -> new UpdateChannelPacketC2S(ConfigHandler.getConfig().getChannel()).send());

		// register world render callback
		WorldRenderEvents.END.register(ctx -> ClientCore.onRenderWorld(ctx.matrixStack(), ctx.projectionMatrix(), ctx.tickDelta()));

		// register gui render callback
		GameOverlayRenderCallback.START.register(ClientCore::onRenderGUI);

		// register commands
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandBuilder.build((context, success, response) -> {
			if (success) {
				context.getSource().sendFeedback(response);
			} else {
				context.getSource().sendError(response);
			}
		})));
	}

	private void registerNetworkPackets() {
		ClientPlayNetworking.registerGlobalReceiver(PingLocationPacketS2C.ID, (a, b, packet, c) -> ClientCore.onPingLocation(packet));
	}

	private void registerReloadListener() {
		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES)
			.registerReloadListener(new IdentifiableResourceReloadListener() {
				@Override
				public Identifier getFabricId() {
					return RELOAD_LISTENER_ID;
				}

				@Override
				public CompletableFuture<Void> reload(Synchronizer helper, ResourceManager resourceManager, Profiler loadProfiler, Profiler applyProfiler, Executor loadExecutor, Executor applyExecutor) {
					return ResourceReloadListener.reloadTextures(helper, resourceManager, loadExecutor, applyExecutor);
				}
			});
	}

	private void registerKeyBindings() {
		KeyBindingHelper.registerKeyBinding(KEY_BINDING_PING);
		KeyBindingHelper.registerKeyBinding(KEY_BINDING_SETTINGS);
		KeyBindingHelper.registerKeyBinding(KEY_BINDING_NAME_LABELS);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (KEY_BINDING_PING.wasPressed()) {
				ClientCore.markLocation();
			}

			if (KEY_BINDING_SETTINGS.wasPressed()) {
				Game.setScreen(new SettingsScreen());
			}
		});
	}
}
