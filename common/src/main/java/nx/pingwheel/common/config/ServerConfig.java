package nx.pingwheel.common.config;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nx.pingwheel.common.core.ServerCore;

@Getter
@Setter
@ToString
@EqualsAndHashCode
public class ServerConfig implements IConfig {
	boolean globalChannelDisabled = false;
	int msToRegenerate = 1000;
	int rateLimit = 5;

	public void validate() {
		if (msToRegenerate < 0) {
			msToRegenerate = 1000;
		}

		if (rateLimit < 0) {
			rateLimit = 0;
		}
	}

	public void onUpdate() {
		ServerCore.init();
	}
}
