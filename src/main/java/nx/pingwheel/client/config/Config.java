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
	int pingDistance = 2048;
	float correctionPeriod = 1f;
	boolean itemIconVisible = true;
	int pingDuration = 7;
	String channel = "";
}
