package nx.pingwheel.common.config;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode
public class Config {
	int pingVolume = 100;
	int pingDuration = 7;
	int pingDistance = 2048;
	int raycastDistance = 1024; // hidden from settings screen
	float correctionPeriod = 1f;
	boolean itemIconVisible = true;
	int pingSize = 100;
	String channel = "";

	public static final Integer MAX_CHANNEL_LENGTH = 128;

	public void validate() {
		if (channel.length() > MAX_CHANNEL_LENGTH) {
			channel = channel.substring(0, MAX_CHANNEL_LENGTH);
		}
	}
}
