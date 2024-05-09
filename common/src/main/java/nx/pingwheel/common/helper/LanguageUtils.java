package nx.pingwheel.common.helper;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import static nx.pingwheel.common.Global.MOD_ID;
import static nx.pingwheel.common.Global.MOD_PREFIX;

public class LanguageUtils {
	private LanguageUtils() {}

	public static final MutableComponent SYMBOL_INFINITE = Component.translatable(MOD_ID + ".symbol.infinite");
	public static final MutableComponent VALUE_HIDDEN = Component.translatable(MOD_ID + ".value.hidden");
	public static final MutableComponent NEWLINE = Component.literal("\n");
	public static final LanguageWrapper UNIT_SECONDS = new LanguageWrapper(MOD_ID + ".unit.seconds");
	public static final LanguageWrapper UNIT_METERS = new LanguageWrapper(MOD_ID + ".unit.meters");
	public static final LanguageWrapper UNIT_PERCENT = new LanguageWrapper(MOD_ID + ".unit.percent");

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

	public static MutableComponent wrapped(MutableComponent component) {
		return Component.empty()
			.append(Component.literal("("))
			.append(component)
			.append(Component.literal(")"));
	}

	public static MutableComponent withModPrefix(MutableComponent component) {
		return Component.empty()
			.append(Component.literal(MOD_PREFIX).withStyle(ChatFormatting.DARK_GRAY))
			.append(component);
	}
}
