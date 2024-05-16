package nx.pingwheel.common.helper;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.network.chat.MutableComponent;
import nx.pingwheel.common.compat.Component;
import nx.pingwheel.common.screen.SettingsScreen;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.function.UnaryOperator;

import static nx.pingwheel.common.ClientGlobal.ConfigHandler;
import static nx.pingwheel.common.ClientGlobal.Game;
import static nx.pingwheel.common.config.ClientConfig.MAX_CHANNEL_LENGTH;

public class ClientCommandBuilder {
	private ClientCommandBuilder() {}

	public static <S> LiteralArgumentBuilder<S> build(TriConsumer<CommandContext<S>, Boolean, MutableComponent> responseHandler) {
		var langHelpFormat = LanguageUtils.command("help.format");
		var langChannel = LanguageUtils.command("channel");
		var langConfig = LanguageUtils.command("config");

		UnaryOperator<String> formatChannel = (channel) -> {
			if (channel.isEmpty()) {
				return langChannel.path("name.default").getString();
			}

			return langChannel.path("name").getString(channel);
		};

		var cmdChannel = LiteralArgumentBuilder.<S>literal("channel")
			.executes((context) -> {
				var currentChannel = ConfigHandler.getConfig().getChannel();

				responseHandler.accept(context, true, langChannel.path("get.response").get(formatChannel.apply(currentChannel)));
				return 1;
			})
			.then(RequiredArgumentBuilder.<S, String>argument("channel_name", StringArgumentType.string()).executes((context) -> {
				var newChannel = context.getArgument("channel_name", String.class);

				if (newChannel.length() > MAX_CHANNEL_LENGTH) {
					responseHandler.accept(context, false, langChannel.path("set.reject").get(MAX_CHANNEL_LENGTH));
					return 0;
				}

				ConfigHandler.getConfig().setChannel(newChannel);
				ConfigHandler.save();

				responseHandler.accept(context, true, langChannel.path("set.response").get(formatChannel.apply(newChannel)));
				return 1;
			}));

		var cmdConfig = LiteralArgumentBuilder.<S>literal("config")
			.executes((context) -> {
				Game.tell(() -> Game.setScreen(new SettingsScreen()));
				return 1;
			});

		Command<S> helpCallback = (context) -> {
			var output = Component.empty();
			output.append(langHelpFormat.getString("/pingwheel config", langConfig.path("description").get()));
			output.append("\n");
			output.append(langHelpFormat.getString("/pingwheel channel", langChannel.path("get.description").get()));
			output.append("\n");
			output.append(langHelpFormat.getString("/pingwheel channel <channel_name>", langChannel.path("set.description").get()));

			responseHandler.accept(context, true, output);
			return 1;
		};

		var cmdHelp = LiteralArgumentBuilder.<S>literal("help")
			.executes(helpCallback);

		return LiteralArgumentBuilder.<S>literal("pingwheel")
			.executes(helpCallback)
			.then(cmdHelp)
			.then(cmdConfig)
			.then(cmdChannel);
	}
}
