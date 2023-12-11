package nx.pingwheel.common.helper;

import com.mojang.serialization.Codec;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;

import java.util.function.*;

public class OptionUtils {
	private OptionUtils() {}

	public static SimpleOption<Integer> ofInt(String key, int min, int max, int step, Function<Integer, Text> formatter, Supplier<Integer> getter, Consumer<Integer> setter) {
		return new SimpleOption<>(
			key,
			SimpleOption.emptyTooltip(),
			(optionText, value) -> formatter.apply(getter.get()),
			(new SimpleOption.ValidatingIntSliderCallbacks(min/step, max/step))
				.withModifier((value) -> value * step, (value) -> value / step),
			Codec.intRange(min, max),
			getter.get(),
			setter
		);
	}

	public static SimpleOption<Float> ofFloat(String key, float min, float max, float step, Function<Float, Text> formatter, Supplier<Float> getter, Consumer<Float> setter) {
		var iMin = (int)(min / step);
		var iMax = (int)(max / step);

		return new SimpleOption<>(
			key,
			SimpleOption.emptyTooltip(),
			(optionText, value) -> formatter.apply(getter.get()),
			(new SimpleOption.ValidatingIntSliderCallbacks(iMin, iMax))
				.withModifier((value) -> value * step, (value) -> (int)(value / step)),
			Codec.floatRange(iMin, iMax),
			getter.get(),
			setter
		);
	}

	public static SimpleOption<Boolean> ofBool(String key, Supplier<Boolean> getter, Consumer<Boolean> setter) {
		return SimpleOption.ofBoolean(
			key,
			getter.get(),
			setter
		);
	}
}
