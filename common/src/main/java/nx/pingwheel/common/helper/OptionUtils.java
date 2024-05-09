package nx.pingwheel.common.helper;

import com.mojang.serialization.Codec;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class OptionUtils {
	private OptionUtils() {}

	public static OptionInstance<Integer> ofInt(String key, int min, int max, int step, Function<Integer, Component> formatter, Supplier<Integer> getter, Consumer<Integer> setter) {
		return new OptionInstance<>(
			key,
			OptionInstance.noTooltip(),
			(optionText, value) -> formatter.apply(getter.get()),
			(new OptionInstance.IntRange(min / step, max / step))
				.xmap((value) -> value * step, (value) -> value / step),
			Codec.intRange(min, max),
			getter.get(),
			setter
		);
	}

	public static OptionInstance<Float> ofFloat(String key, float min, float max, float step, Function<Float, Component> formatter, Supplier<Float> getter, Consumer<Float> setter) {
		var iMin = (int) (min / step);
		var iMax = (int) (max / step);

		return new OptionInstance<>(
			key,
			OptionInstance.noTooltip(),
			(optionText, value) -> formatter.apply(getter.get()),
			(new OptionInstance.IntRange(iMin, iMax))
				.xmap((value) -> value * step, (value) -> (int) (value / step)),
			Codec.floatRange(iMin, iMax),
			getter.get(),
			setter
		);
	}

	public static OptionInstance<Boolean> ofBool(String key, Supplier<Boolean> getter, Consumer<Boolean> setter) {
		return OptionInstance.createBoolean(
			key,
			getter.get(),
			setter
		);
	}
}
