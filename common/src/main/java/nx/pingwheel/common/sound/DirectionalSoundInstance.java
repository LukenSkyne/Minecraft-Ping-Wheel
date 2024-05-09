package nx.pingwheel.common.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import static nx.pingwheel.common.ClientGlobal.Game;

public class DirectionalSoundInstance extends AbstractTickableSoundInstance {

	private final Vec3 pos;

	public DirectionalSoundInstance(SoundEvent sound,
									SoundSource category,
									float volume,
									float pitch,
									Vec3 pos) {
		super(sound, category, RandomSource.create(0));

		this.volume = volume;
		this.pitch = pitch;
		this.pos = pos;
		tick();
	}

	@Override
	public void tick() {
		if (Game.player == null) {
			this.stop();
			return;
		}

		var playerPos = Game.player.position();
		var vecBetween = playerPos.vectorTo(this.pos);
		var mappedDistance = Math.min(vecBetween.length(), 64.0) / 64.0 * 14.0;
		var soundDirection = vecBetween.normalize().scale(mappedDistance);
		var soundPos = playerPos.add(soundDirection);

		this.x = soundPos.x;
		this.y = soundPos.y;
		this.z = soundPos.z;
	}
}
