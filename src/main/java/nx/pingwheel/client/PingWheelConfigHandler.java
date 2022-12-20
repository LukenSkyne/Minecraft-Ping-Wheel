package nx.pingwheel.client;

public class PingWheelConfigHandler extends ConfigHandler {

	public static PingWheelConfigHandler getInstance() {
		if (instance == null) {
			instance = new PingWheelConfigHandler();
		}

		return instance;
	}

	protected PingWheelConfigHandler() {
		super("ping-wheel.json");
	}

	private static PingWheelConfigHandler instance;
}
