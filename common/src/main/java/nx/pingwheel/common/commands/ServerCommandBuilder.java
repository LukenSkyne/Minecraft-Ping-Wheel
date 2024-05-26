package nx.pingwheel.common.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.MutableComponent;
import nx.pingwheel.common.compat.Component;
import nx.pingwheel.common.helper.ChannelMode;
import nx.pingwheel.common.helper.LanguageUtils;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static nx.pingwheel.common.Global.ServerConfigHandler;

public class ServerCommandBuilder {
	private ServerCommandBuilder() {}

	public static LiteralArgumentBuilder<CommandSourceStack> build(TriConsumer<CommandContext<CommandSourceStack>, Boolean, MutableComponent> responseHandler) {
		var langDefaultChannel = LanguageUtils.command("default_channel");
		var validModes = List.of(ChannelMode.values());
		var validModeNames = validModes.stream().map(ChannelMode::toString).toList();

		var cmdMode = LiteralArgumentBuilder.<CommandSourceStack>literal("default_channel")
			.executes((context) -> {
				var currentChannelMode = ServerConfigHandler.getConfig().getDefaultChannelMode();

				responseHandler.accept(context, true, langDefaultChannel.path("get.response")
					.get(langDefaultChannel.path("value").path(currentChannelMode.toString()).get().withStyle(ChatFormatting.YELLOW)));
				return 1;
			})
			.then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("mode_name", StringArgumentType.string())
				.suggests((context, builder) -> CompletableFuture.supplyAsync(() -> {
					for (var mode : validModeNames) {
						builder.suggest(mode);
					}
					return builder.build();
				}))
				.executes((context) -> {
					var newModeStr = context.getArgument("mode_name", String.class);
					var newMode = validModes.stream().filter(e -> e.name().equalsIgnoreCase(newModeStr)).findFirst().orElse(null);

					if (newMode == null) {
						responseHandler.accept(context, false, langDefaultChannel.path("set.reject")
							.get(String.join(" | ", validModeNames)));
						return 0;
					}

					ServerConfigHandler.getConfig().setDefaultChannelMode(newMode);
					ServerConfigHandler.save();

					responseHandler.accept(context, true, langDefaultChannel.path("set.response")
						.get(langDefaultChannel.path("value").path(newMode.toString()).get().withStyle(ChatFormatting.YELLOW)));
					return 1;
				}));

		Command<CommandSourceStack> helpCallback = (context) -> {
			responseHandler.accept(context, true, LanguageUtils.join(
				Component.literal("/pingwheel:server default_channel"),
				LanguageUtils.wrapped(langDefaultChannel.path("get.description").get()).withStyle(ChatFormatting.GRAY),
				Component.literal("/pingwheel:server default_channel <mode_name>"),
				LanguageUtils.wrapped(langDefaultChannel.path("set.description").get()).withStyle(ChatFormatting.GRAY)
			));
			return 1;
		};

		var cmdHelp = LiteralArgumentBuilder.<CommandSourceStack>literal("help")
			.executes(helpCallback);

		return LiteralArgumentBuilder.<CommandSourceStack>literal("pingwheel:server")
			.requires(source -> source.hasPermission(2))
			.executes(helpCallback)
			.then(cmdHelp)
			.then(cmdMode);
	}
}
