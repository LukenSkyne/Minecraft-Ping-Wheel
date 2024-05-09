package nx.pingwheel.common.helper;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import nx.pingwheel.common.screen.SettingsScreen;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

import static nx.pingwheel.common.ClientGlobal.ConfigHandler;
import static nx.pingwheel.common.ClientGlobal.Game;
import static nx.pingwheel.common.config.ClientConfig.MAX_CHANNEL_LENGTH;

public class ClientCommandBuilder {
	private ClientCommandBuilder() {}

	public static <S> LiteralArgumentBuilder<S> build(TriConsumer<CommandContext<S>, Boolean, MutableComponent> responseHandler) {
		UnaryOperator<String> formatChannel = (channel) -> {
			if (channel.isEmpty()) {
				return Component.translatable("ping-wheel.command.channel.name.default").getString();
			}

			return Component.translatable("ping-wheel.command.channel.name", channel).getString();
		};

		var cmdChannel = LiteralArgumentBuilder.<S>literal("channel")
			.executes((context) -> {
				var currentChannel = ConfigHandler.getConfig().getChannel();

				responseHandler.accept(context, true, Component.translatable("ping-wheel.command.channel.get.response", formatChannel.apply(currentChannel)));
				return 1;
			})
			.then(RequiredArgumentBuilder.<S, String>argument("channel_name", StringArgumentType.string()).executes((context) -> {
				var newChannel = context.getArgument("channel_name", String.class);

				if (newChannel.length() > MAX_CHANNEL_LENGTH) {
					responseHandler.accept(context, false, Component.translatable("ping-wheel.command.channel.set.reject", MAX_CHANNEL_LENGTH));
					return 0;
				}

				ConfigHandler.getConfig().setChannel(newChannel);
				ConfigHandler.save();

				responseHandler.accept(context, true, Component.translatable("ping-wheel.command.channel.set.response", formatChannel.apply(newChannel)));
				return 1;
			}));

		var cmdConfig = LiteralArgumentBuilder.<S>literal("config")
			.executes((context) -> {
				Game.tell(() -> Game.setScreen(new SettingsScreen()));
				return 1;
			});

		Command<S> helpCallback = (context) -> {
			BinaryOperator<String> form = (command, key) -> Component.translatable("ping-wheel.command.help.format", command, Component.translatable(key)).getString();

			var output = Component.empty();
			output.append(form.apply("/pingwheel config", "ping-wheel.command.config.description"));
			output.append("\n");
			output.append(form.apply("/pingwheel channel", "ping-wheel.command.channel.get.description"));
			output.append("\n");
			output.append(form.apply("/pingwheel channel <channel_name>", "ping-wheel.command.channel.set.description"));

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
