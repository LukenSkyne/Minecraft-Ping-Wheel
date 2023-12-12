package nx.pingwheel.common.helper;

import net.minecraft.client.option.CyclingOption;
import net.minecraft.client.option.DoubleOption;
import net.minecraft.client.option.Option;
import net.minecraft.text.Text;

import java.util.function.*;

public class OptionUtils {
	private OptionUtils() {}

	public static Option ofInt(String key, int min, int max, int step, Function<Integer, Text> formatter, Supplier<Integer> getter, Consumer<Integer> setter) {
		return new DoubleOption(
			key, min, max, step,
			(gameOptions) -> (double)getter.get(),
			(gameOptions, value) -> setter.accept(value.intValue()),
			(gameOptions, option) -> formatter.apply(getter.get())
		);
	}

	public static Option ofFloat(String key, float min, float max, float step, Function<Float, Text> formatter, Supplier<Float> getter, Consumer<Float> setter) {
		return new DoubleOption(
			key, min, max, step,
			(gameOptions) -> (double)getter.get(),
			(gameOptions, value) -> setter.accept(value.floatValue()),
			(gameOptions, option) -> formatter.apply(getter.get())
		);
	}

	public static Option ofBool(String key, Supplier<Boolean> getter, Consumer<Boolean> setter) {
		return CyclingOption.create(
			key,
			(gameOptions) -> getter.get(),
			(gameOptions, option, value) -> setter.accept(value)
		);
	}
}
