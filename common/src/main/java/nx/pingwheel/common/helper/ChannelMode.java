package nx.pingwheel.common.helper;

public enum ChannelMode {
	AUTO, DISABLED, GLOBAL, TEAM_ONLY;

	public static ChannelMode get(String name) {
		for (ChannelMode mode : ChannelMode.values()) {
			if (mode.name().equalsIgnoreCase(name)) {
				return mode;
			}
		}

		return null;
	}

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}
