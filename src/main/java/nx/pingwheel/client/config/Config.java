package nx.pingwheel.client.config;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.Setter;

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
}
