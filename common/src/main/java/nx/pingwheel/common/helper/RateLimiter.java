package nx.pingwheel.common.helper;

import java.time.Duration;
import java.time.Instant;

public class RateLimiter {

	private static Duration timeToRegenerate;
	private static Duration timeWindow;

	public static void setRates(long timeToRegenerate, long limit) {
		RateLimiter.timeToRegenerate = Duration.ofSeconds(timeToRegenerate);
		RateLimiter.timeWindow = Duration.ofSeconds(timeToRegenerate * limit);
	}

	private Instant startTime;

	public RateLimiter() {
		this.startTime = Instant.now().minus(timeWindow);
	}

	public boolean checkAndBlock() {
		final var now = Instant.now();
		var elapsed = Duration.between(startTime, now);

		if (elapsed.compareTo(timeWindow) > 0) {
			elapsed = timeWindow;
		}

		var leftOver = elapsed.minus(timeToRegenerate);

		if (leftOver.isNegative()) {
			return true;
		}

		startTime = now.minus(leftOver);

		return false;
	}
}
