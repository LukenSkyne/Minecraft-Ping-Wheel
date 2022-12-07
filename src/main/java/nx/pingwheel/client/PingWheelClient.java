package nx.pingwheel.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import nx.pingwheel.shared.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class PingWheelClient implements ClientModInitializer {

	public static final Logger LOGGER = LogManager.getLogger("nx-ping-wheel");

	private static KeyBinding kbPing;

	@Override
	public void onInitializeClient() {
		LOGGER.info("Client Init");

		SetupKeyBindings();

		ClientPlayNetworking.registerGlobalReceiver(Constants.S2C_PING_LOCATION, Core::onReceivePing);
	}

	private void SetupKeyBindings() {
		kbPing = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.ping-wheel.ping", InputUtil.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_5, "key.ping-wheel.main"));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (kbPing.wasPressed()) {
				Core.doPing();
			}
		});
	}
}
