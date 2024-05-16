package nx.pingwheel.common.helper;

import net.minecraft.network.chat.MutableComponent;
import nx.pingwheel.common.compat.Component;

import static nx.pingwheel.common.Global.MOD_ID;

public class LanguageUtils {
	private LanguageUtils() {}

	public static final MutableComponent SYMBOL_INFINITE = Component.translatable(MOD_ID + ".symbol.infinite");
	public static final MutableComponent VALUE_HIDDEN = Component.translatable(MOD_ID + ".value.hidden");

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
}
