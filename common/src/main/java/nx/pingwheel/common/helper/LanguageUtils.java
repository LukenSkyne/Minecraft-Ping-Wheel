package nx.pingwheel.common.helper;

import net.minecraft.network.chat.MutableComponent;
import nx.pingwheel.common.compat.Component;

import static nx.pingwheel.common.Global.MOD_ID;

public class LanguageUtils {
	private LanguageUtils() {}

	public static final MutableComponent SYMBOL_INFINITE = Component.translatable(MOD_ID + ".symbol.infinite");
	public static final MutableComponent VALUE_HIDDEN = Component.translatable(MOD_ID + ".value.hidden");
	public static final MutableComponent NEWLINE = Component.nullToEmpty("\n");

	public static LanguageWrapper settings(String key) {
		return new LanguageWrapper(MOD_ID + ".settings." + key);
	}

	public static LanguageWrapper command(String key) {
		return new LanguageWrapper(MOD_ID + ".command." + key);
	}

	public record LanguageWrapper(String key) {

		public LanguageWrapper path(String key) {
			return new LanguageWrapper(this.key + "." + key);
		}

		public MutableComponent get(Object... args) {
			return Component.translatable(this.key, args);
		}

		public String getString(Object... args) {
			return this.get(args).getString();
		}
	}

	public static MutableComponent join(MutableComponent... components) {
		var output = Component.empty();

		for (int i = 0; i < components.length; i++) {
			var component = components[i];

			if (i > 0) {
				output.append(NEWLINE);
			}

			output.append(component);
		}

		return output;
	}

	public static MutableComponent wrapped(MutableComponent component, String... delimiter) {
		if (delimiter.length == 0) {
			delimiter = new String[] {"(", ")"};
		}

		return Component.empty()
			.append(Component.nullToEmpty(delimiter[0]))
			.append(component)
			.append(Component.nullToEmpty(delimiter[delimiter.length - 1]));
	}
}
