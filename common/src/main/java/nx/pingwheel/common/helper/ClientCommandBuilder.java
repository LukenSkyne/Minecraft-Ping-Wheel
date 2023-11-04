package nx.pingwheel.common.helper;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.text.Text;
import nx.pingwheel.common.screen.SettingsScreen;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.function.UnaryOperator;

import static nx.pingwheel.common.ClientGlobal.ConfigHandler;
import static nx.pingwheel.common.ClientGlobal.Game;
import static nx.pingwheel.common.config.Config.MAX_CHANNEL_LENGTH;

public class ClientCommandBuilder {
	private ClientCommandBuilder() {}

	public static <S> LiteralArgumentBuilder<S> build(TriConsumer<CommandContext<S>, Boolean, Text> responseHandler) {
		UnaryOperator<String> formatChannel = (channel) -> channel.isEmpty() ? "§eGlobal §7(default)" : String.format("\"§6%s§f\"", channel);

		var cmdChannel = LiteralArgumentBuilder.<S>literal("channel")
			.executes((context) -> {
				var currentChannel = ConfigHandler.getConfig().getChannel();

				responseHandler.accept(context, true, Text.of(String.format("§fCurrent Ping-Wheel channel: %s", formatChannel.apply(currentChannel))));

				return 1;
			})
			.then(RequiredArgumentBuilder.<S, String>argument("channel_name", StringArgumentType.string()).executes((context) -> {
				var newChannel = context.getArgument("channel_name", String.class);

				if (newChannel.length() > MAX_CHANNEL_LENGTH) {
					responseHandler.accept(context, false, Text.of(String.format("Channel names are limited to a length of %s characters", MAX_CHANNEL_LENGTH)));
					return 0;
				}

				ConfigHandler.getConfig().setChannel(newChannel);
				ConfigHandler.save();

				responseHandler.accept(context, true, Text.of(String.format("§fSet Ping-Wheel channel to: %s", formatChannel.apply(newChannel))));

				return 1;
			}));

		var cmdConfig = LiteralArgumentBuilder.<S>literal("config")
			.executes((context) -> {
				Game.send(() -> Game.setScreen(new SettingsScreen()));

				return 1;
			});

		Command<S> helpCallback = (context) -> {
			var output = """
				§f/pingwheel config
				§7(manage pingwheel configuration)
				§f/pingwheel channel
				§7(get your current channel)
				§f/pingwheel channel <channel_name>
				§7(set your current channel, use "" for global channel)""";

			responseHandler.accept(context, true, Text.of(output));

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
