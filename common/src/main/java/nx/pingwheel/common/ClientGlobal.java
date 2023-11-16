package nx.pingwheel.common;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import nx.pingwheel.common.config.ConfigHandler;
import org.lwjgl.glfw.GLFW;

import static nx.pingwheel.common.Global.MOD_ID;

@Environment(EnvType.CLIENT)
public class ClientGlobal {
	private ClientGlobal() {}

	public static ConfigHandler ConfigHandler = null;
	public static final MinecraftClient Game = MinecraftClient.getInstance();

	public static final Identifier PING_SOUND_ID = new Identifier(MOD_ID, "ping");
	public static final SoundEvent PING_SOUND_EVENT = SoundEvent.of(PING_SOUND_ID);
	public static final Identifier PING_TEXTURE_ID = new Identifier(MOD_ID, "textures/ping.png");

	public static final KeyBinding KEY_BINDING_PING = new KeyBinding("ping-wheel.key.mark-location", InputUtil.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_5, "ping-wheel.name");
	public static final KeyBinding KEY_BINDING_SETTINGS = new KeyBinding("ping-wheel.key.open-settings", InputUtil.Type.KEYSYM, -1, "ping-wheel.name");
}
