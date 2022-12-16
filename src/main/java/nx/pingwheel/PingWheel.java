package nx.pingwheel;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;
import nx.pingwheel.shared.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PingWheel implements ModInitializer {

	public static final Logger LOGGER = LogManager.getLogger("nx-ping-wheel");

	public static final Identifier PING_SOUND_ID = new Identifier("ping-wheel:ping");
	public static SoundEvent PING_SOUND_EVENT = SoundEvent.of(PING_SOUND_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("[Ping-Wheel] Server Init");

		Registry.register(Registries.SOUND_EVENT, PING_SOUND_ID, PING_SOUND_EVENT);

		ServerPlayNetworking.registerGlobalReceiver(Constants.C2S_PING_LOCATION, (server, player, handler, buf, responseSender) -> {
			final var packet = PacketByteBufs.copy(buf);

			for (ServerPlayerEntity p : PlayerLookup.world(player.getWorld())) {
				ServerPlayNetworking.send(p, Constants.S2C_PING_LOCATION, packet);
			}
		});
	}
}
