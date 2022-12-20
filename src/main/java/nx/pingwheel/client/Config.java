package nx.pingwheel.client;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Setter;

@Setter
@ToString
@EqualsAndHashCode
public class Config {
	int pingVolume = 100;
	int pingDistance = 2048;
	boolean itemIconVisible = true;
	String channel = "";

	// manual getters for kotlin compatibility

	public int getPingVolume() {
		return pingVolume;
	}

	public int getPingDistance() {
		return pingDistance;
	}

	public boolean isItemIconVisible() {
		return itemIconVisible;
	}

	public String getChannel() {
		return channel;
	}
}
