package nx.pingwheel.common.helper;

import net.minecraft.client.CycleOption;
import net.minecraft.client.Option;
import net.minecraft.client.ProgressOption;
import net.minecraft.network.chat.Component;

import java.util.function.*;

public class OptionUtils {
	private OptionUtils() {}

	public static Option ofInt(String key, int min, int max, int step, Function<Integer, Component> formatter, Supplier<Integer> getter, Consumer<Integer> setter) {
		return new ProgressOption(
			key, min, max, step,
			(gameOptions) -> (double)getter.get(),
			(gameOptions, value) -> setter.accept(value.intValue()),
			(gameOptions, option) -> formatter.apply(getter.get())
		);
	}

	public static Option ofFloat(String key, float min, float max, float step, Function<Float, Component> formatter, Supplier<Float> getter, Consumer<Float> setter) {
		return new ProgressOption(
			key, min, max, step,
			(gameOptions) -> (double)getter.get(),
			(gameOptions, value) -> setter.accept(value.floatValue()),
			(gameOptions, option) -> formatter.apply(getter.get())
		);
	}

	public static Option ofBool(String key, Supplier<Boolean> getter, Consumer<Boolean> setter) {
		return CycleOption.createOnOff(
			key,
			(gameOptions) -> getter.get(),
			(gameOptions, option, value) -> setter.accept(value)
		);
	}
}
