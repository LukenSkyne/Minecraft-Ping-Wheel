package nx.pingwheel.common.config;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nx.pingwheel.common.networking.UpdateChannelPacketC2S;

@Getter
@Setter
@ToString
@EqualsAndHashCode
public class ClientConfig implements IConfig {
	int pingVolume = 100;
	int pingDuration = 7;
	int pingDistance = 2048;
	float correctionPeriod = 1f;
	boolean itemIconVisible = true;
	boolean directionIndicatorVisible = true;
	int pingSize = 100;
	String channel = "";

	// hidden from settings screen
	int raycastDistance = 1024;
	int safeZoneLeft = 5;
	int safeZoneRight = 5;
	int safeZoneTop = 5;
	int safeZoneBottom = 60;

	public static final Integer MAX_CHANNEL_LENGTH = 128;

	public void validate() {
		if (channel.length() > MAX_CHANNEL_LENGTH) {
			channel = channel.substring(0, MAX_CHANNEL_LENGTH);
		}
	}

	public void onUpdate() {
		new UpdateChannelPacketC2S(channel).send();
	}
}
