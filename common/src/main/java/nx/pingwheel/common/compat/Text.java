package nx.pingwheel.common.compat;

import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableText;

public interface Text {

	static MutableText empty() {
		return (MutableText)LiteralText.EMPTY;
	}

	static MutableText translatable(String key, Object... args) {
		return new TranslatableText(key, args);
	}
}
