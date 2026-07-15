package nx.pingwheel.common.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import nx.pingwheel.common.config.ClientConfig;
import nx.pingwheel.common.config.PlayerInfoMode;
import nx.pingwheel.common.config.TeamColorMode;
import nx.pingwheel.common.integration.TeamContext;
import nx.pingwheel.common.integration.TeamContextHandler;
import nx.pingwheel.common.resource.LanguageUtils;

import static nx.pingwheel.common.CommonClient.Game;
import static nx.pingwheel.common.config.ClientConfig.*;

public class SettingsScreen extends OptionsSubScreen {

	private static final int WHITE = 0xFFFFFFFF;
	private static final int GRAY = 0xFFA0A0A0;

	private final ClientConfig config;

	private Screen parent;
	private EditBox channelTextField;

	public SettingsScreen() {
		super(null, null, LanguageUtils.settings("title").get());
		this.config = ClientConfig.HANDLER.getConfig();
	}

	public SettingsScreen(Screen parent) {
		this();
		this.parent = parent;
	}

	@Override
	public void tick() {
		if (this.channelTextField.isFocused() && this.getFocused() != this.channelTextField) {
			this.setFocused(this.channelTextField);
		}
	}

	@Override
	protected void init() {
		this.addTitle();
		this.addContents();
		this.addFooter();
		this.layout.visitWidgets(this::addRenderableWidget);
		this.repositionElements();
	}

	@Override
	protected void addContents() {
		this.list = this.layout.addToContents(new OptionsList(this.minecraft, this.width, this));
		this.addOptions();
	}

	@Override
	protected void addOptions() {
		this.list.addSmall(getPingVolumeOption(), getPingDurationOption());

		this.list.addSmall(getPingDistanceOption(), getCorrectionPeriodOption());

		this.list.addSmall(getItemIconsVisibleOption(), getDirectionIndicatorVisibleOption());

		this.list.addSmall(getPlayerInfoModeOption(), getTeamColorModeOption());

		this.list.addSmall(getPingSizeOption(), null);

		this.channelTextField = new EditBox(this.font, -1, -1, 200, 20, Component.empty());
		this.channelTextField.setMaxLength(MAX_CHANNEL_LENGTH);
		this.channelTextField.setValue(config.getChannel());
		this.channelTextField.setResponder(config::setChannel);
		this.addWidget(this.channelTextField);
	}

	@Override
	public void onClose() {
		ClientConfig.HANDLER.save();

		if (parent != null && this.minecraft != null) {
			this.minecraft.gui.setScreen(parent);
		} else {
			super.onClose();
		}
	}

	@Override
	public void repositionElements() {
		super.repositionElements();
		this.list.updateSize(this.width, this.layout);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
		super.extractRenderState(ctx, mouseX, mouseY, delta);
		this.list.extractRenderState(ctx, mouseX, mouseY, delta);

		final var yOffset = 50 + 25 * this.list.children().size();
		this.channelTextField.setPosition(width / 2 - 100, yOffset);
		ctx.text(this.font, LanguageUtils.settings("channel").get(), this.width / 2 - 100, this.channelTextField.getY() - 12, GRAY);
		this.channelTextField.extractRenderState(ctx, mouseX, mouseY, delta);

		if (this.channelTextField.getValue().isEmpty() && !this.channelTextField.isFocused()) {
			ctx.text(this.font, getChannelPlaceholder(), this.width / 2 - 100 + 4, this.channelTextField.getY() + 6, WHITE);
		}

		if (this.channelTextField.isHoveredOrFocused() && !this.channelTextField.isFocused()) {
			final var clientTooltipComponentList = Tooltip.create(LanguageUtils.settings("channel.tooltip").get()).toCharSequence(Game)
				.stream()
				.map(ClientTooltipComponent::create)
				.toList();

			ctx.tooltip(this.font, clientTooltipComponentList, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
		}
	}

	private MutableComponent getChannelPlaceholder() {
		if (Game.player == null) {
			return Component.empty();
		}

		final var teamContext = TeamContextHandler.getSelfContext();
		MutableComponent placeholder;

		if (teamContext == TeamContext.NONE) {
			placeholder = LanguageUtils.of("value", "global").get();
		} else {
			placeholder = LanguageUtils.settings("channel").path("placeholder")
				.get(LanguageUtils.of("value", teamContext.toString()).get());
		}

		return placeholder
			.withStyle(ChatFormatting.ITALIC)
			.withStyle(ChatFormatting.DARK_GRAY);
	}

	private OptionInstance<Integer> getPingVolumeOption() {
		final var text = LanguageUtils.settings("ping_volume");

		return OptionUtils.ofInt(
			text.getKey(),
			0, 100, 1,
			(value) -> {
				if (value == 0) {
					return text.get(CommonComponents.OPTION_OFF);
				}

				return text.get(LanguageUtils.UNIT_PERCENT.get(value));
			},
			config::getPingVolume,
			config::setPingVolume
		);
	}

	private OptionInstance<Integer> getPingDurationOption() {
		final var text = LanguageUtils.settings("ping_duration");

		return OptionUtils.ofInt(
			text.getKey(),
			1, MAX_PING_DURATION, 1,
			(value) -> {
				if (value >= MAX_PING_DURATION) {
					return text.get(LanguageUtils.VALUE_INFINITE);
				}

				return text.get(LanguageUtils.UNIT_SECONDS.get(value));
			},
			config::getPingDuration,
			config::setPingDuration
		);
	}

	private OptionInstance<Integer> getPingDistanceOption() {
		final var text = LanguageUtils.settings("ping_distance");

		return OptionUtils.ofInt(
			text.getKey(),
			0, MAX_PING_DISTANCE, 16,
			(value) -> {
				if (value == 0) {
					return text.get(LanguageUtils.VALUE_HIDDEN);
				} else if (value >= MAX_PING_DISTANCE) {
					return text.get(LanguageUtils.VALUE_INFINITE);
				}

				return text.get(LanguageUtils.UNIT_METERS.get(value));
			},
			config::getPingDistance,
			config::setPingDistance
		);
	}

	private OptionInstance<Float> getCorrectionPeriodOption() {
		final var text = LanguageUtils.settings("correction_period");

		return OptionUtils.ofFloat(
			text.getKey(),
			0.1f, MAX_CORRECTION_PERIOD, 0.1f,
			(value) -> {
				if (value >= MAX_CORRECTION_PERIOD) {
					return text.get(LanguageUtils.VALUE_INFINITE);
				}

				return text.get(LanguageUtils.UNIT_SECONDS.get("%.1f".formatted(value)));
			},
			config::getCorrectionPeriod,
			config::setCorrectionPeriod
		);
	}

	private OptionInstance<Boolean> getItemIconsVisibleOption() {
		return OptionUtils.ofBool(
			LanguageUtils.settings("item_icon_visible").getKey(),
			config::isItemIconVisible,
			config::setItemIconVisible
		);
	}

	private OptionInstance<Boolean> getDirectionIndicatorVisibleOption() {
		return OptionUtils.ofBool(
			LanguageUtils.settings("direction_indicator_visible").getKey(),
			config::isDirectionIndicatorVisible,
			config::setDirectionIndicatorVisible
		);
	}

	private OptionInstance<PlayerInfoMode> getPlayerInfoModeOption() {
		return OptionUtils.ofEnum(
			LanguageUtils.settings("player_info_mode").getKey(),
			PlayerInfoMode.class,
			(mode) -> LanguageUtils.of("value", mode.toString()).get(),
			(mode) -> {
				if (mode != PlayerInfoMode.HOLD) return Component.empty();

				final var kayPlayerListTitle = Component.translatable(Game.options.keyPlayerList.getName());
				final var kayPlayerListName = Game.options.keyPlayerList.getTranslatedKeyMessage();

				return LanguageUtils.settings("player_info_mode")
					.path("hold", "tooltip")
					.get(kayPlayerListTitle, kayPlayerListName);
			},
			config::getPlayerInfoMode,
			config::setPlayerInfoMode
		);
	}

	private OptionInstance<TeamColorMode> getTeamColorModeOption() {
		return OptionUtils.ofEnum(
			LanguageUtils.settings("team_color_mode").getKey(),
			TeamColorMode.class,
			(mode) -> LanguageUtils.of("value", mode.toString()).get(),
			(mode) -> Component.empty(),
			config::getTeamColorMode,
			config::setTeamColorMode
		);
	}

	private OptionInstance<Integer> getPingSizeOption() {
		final var text = LanguageUtils.settings("ping_size");

		return OptionUtils.ofInt(
			text.getKey(),
			40, 300, 10,
			(value) -> text.get(LanguageUtils.UNIT_PERCENT.get(value)),
			config::getPingSize,
			config::setPingSize
		);
	}
}
