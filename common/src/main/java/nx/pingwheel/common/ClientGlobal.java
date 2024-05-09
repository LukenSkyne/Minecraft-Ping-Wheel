package nx.pingwheel.common;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import nx.pingwheel.common.config.ClientConfig;
import nx.pingwheel.common.config.ConfigHandler;
import org.lwjgl.glfw.GLFW;

import static nx.pingwheel.common.Global.MOD_ID;

public class ClientGlobal {
	private ClientGlobal() {}

	public static ConfigHandler<ClientConfig> ConfigHandler = null;
	public static final Minecraft Game = Minecraft.getInstance();

	public static final ResourceLocation PING_SOUND_ID = new ResourceLocation(MOD_ID, "ping");
	public static final SoundEvent PING_SOUND_EVENT = SoundEvent.createVariableRangeEvent(PING_SOUND_ID);
	public static final ResourceLocation PING_TEXTURE_ID = new ResourceLocation(MOD_ID, "textures/ping.png");

	private static final String SETTINGS_CATEGORY = "ping-wheel.name";
	public static final KeyMapping KEY_BINDING_PING = new KeyMapping("ping-wheel.key.ping-location", InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_5, SETTINGS_CATEGORY);
	public static final KeyMapping KEY_BINDING_SETTINGS = new KeyMapping("ping-wheel.key.open-settings", InputConstants.Type.KEYSYM, -1, SETTINGS_CATEGORY);
	public static final KeyMapping KEY_BINDING_NAME_LABELS = new KeyMapping("ping-wheel.key.name-labels", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, SETTINGS_CATEGORY);
}
