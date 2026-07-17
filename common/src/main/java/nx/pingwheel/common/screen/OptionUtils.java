package nx.pingwheel.common.screen;

import com.mojang.serialization.Codec;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.List;
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
				.xmap((value) -> value * step, (value) -> value / step, true),
			Codec.intRange(min, max),
			getter.get(),
			setter::accept
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
				.xmap((value) -> value * step, (value) -> (int) (value / step), true),
			Codec.floatRange(iMin, iMax),
			getter.get(),
			setter::accept
		);
	}

	public static OptionInstance<Boolean> ofBool(String key, Supplier<Boolean> getter, Consumer<Boolean> setter) {
		return OptionInstance.createBoolean(
			key,
			getter.get(),
			setter::accept
		);
	}

	public static <E extends Enum<E>> OptionInstance<E> ofEnum(String key, Class<E> enumClass, Function<E, Component> formatter, Function<E, Component> tooltipSupplier, Supplier<E> getter, Consumer<E> setter) {
		return new OptionInstance<>(
			key,
			(mode) -> Tooltip.create(tooltipSupplier.apply(mode)),
			(optionText, value) -> formatter.apply(value),
			new OptionInstance.Enum<>(List.of(enumClass.getEnumConstants()), Codec.STRING.xmap(
				name -> Enum.valueOf(enumClass, name),
				Enum::name
			)),
			getter.get(),
			setter::accept
		);
	}
}
