package nx.pingwheel.common.screen;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import nx.pingwheel.common.config.ClientConfig;
import nx.pingwheel.common.helper.OptionUtils;

import static nx.pingwheel.common.ClientGlobal.ConfigHandler;
import static nx.pingwheel.common.config.ClientConfig.MAX_CHANNEL_LENGTH;

public class SettingsScreen extends OptionsSubScreen {

	private final ClientConfig config;

	private Screen parent;
	private OptionsList list;
	private EditBox channelTextField;

	public SettingsScreen() {
		super(null, null, Component.translatable("ping-wheel.settings.title"));
		this.config = ConfigHandler.getConfig();
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
		this.list = new OptionsList(this.minecraft, this.width, this.height, this);

		final var pingVolumeOption = getPingVolumeOption();
		final var pingDurationOption = getPingDurationOption();
		this.list.addSmall(pingVolumeOption, pingDurationOption);

		final var pingDistanceOption = getPingDistanceOption();
		final var correctionPeriodOption = getCorrectionPeriodOption();
		this.list.addSmall(pingDistanceOption, correctionPeriodOption);

		final var itemIconsVisibleOption = getItemIconsVisibleOption();
		final var directionIndicatorVisibleOption = getDirectionIndicatorVisibleOption();
		this.list.addSmall(itemIconsVisibleOption, directionIndicatorVisibleOption);

		final var nameLabelForcedOption = getNameLabelForcedOption();
		final var pingSizeOption = getPingSizeOption();
		this.list.addSmall(nameLabelForcedOption, pingSizeOption);

		this.channelTextField = new EditBox(this.font, 0, 0, 200, 20, Component.empty());
		this.channelTextField.setMaxLength(MAX_CHANNEL_LENGTH);
		this.channelTextField.setValue(config.getChannel());
		this.channelTextField.setResponder(config::setChannel);
		this.addWidget(this.channelTextField);

		this.addWidget(this.list);

		super.init();
	}

	@Override
	public void onClose() {
		ConfigHandler.save();

		if (parent != null && this.minecraft != null) {
			this.minecraft.setScreen(parent);
		} else {
			super.onClose();
		}
	}

	@Override
	public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);
		this.list.render(ctx, mouseX, mouseY, delta);

		ctx.drawString(this.font, Component.translatable("ping-wheel.settings.channel"), this.width / 2 - 100, 148, 10526880);
		this.channelTextField.setPosition(this.width / 2 - 100, 160);
		this.channelTextField.render(ctx, mouseX, mouseY, delta);

		if (this.channelTextField.isHoveredOrFocused() && !this.channelTextField.isFocused()) {
			ctx.renderTooltip(this.font, this.font.split(Component.translatable("ping-wheel.settings.channel.tooltip"), 140), mouseX, mouseY);
		}
	}

	private OptionInstance<Integer> getPingVolumeOption() {
		final var pingVolumeKey = "ping-wheel.settings.pingVolume";

		return OptionUtils.ofInt(
			pingVolumeKey,
			0, 100, 1,
			(value) -> {
				if (value == 0) {
					return Component.translatable(pingVolumeKey, CommonComponents.OPTION_OFF);
				}

				return Component.translatable(pingVolumeKey, String.format("%s%%", value));
			},
			config::getPingVolume,
			config::setPingVolume
		);
	}

	private OptionInstance<Integer> getPingDurationOption() {
		final var pingDurationKey = "ping-wheel.settings.pingDuration";

		return OptionUtils.ofInt(
			pingDurationKey,
			1, 60, 1,
			(value) -> Component.translatable(pingDurationKey, String.format("%ss", config.getPingDuration())),
			config::getPingDuration,
			config::setPingDuration
		);
	}

	private OptionInstance<Integer> getPingDistanceOption() {
		final var pingDistanceKey = "ping-wheel.settings.pingDistance";

		return OptionUtils.ofInt(
			pingDistanceKey,
			0, 2048, 16,
			(value) -> {
				if (value == 0) {
					return Component.translatable(pingDistanceKey, Component.translatable(pingDistanceKey + ".hidden"));
				} else if (value == 2048) {
					return Component.translatable(pingDistanceKey, Component.translatable(pingDistanceKey + ".unlimited"));
				}

				return Component.translatable(pingDistanceKey, String.format("%sm", value));
			},
			config::getPingDistance,
			config::setPingDistance
		);
	}

	private OptionInstance<Float> getCorrectionPeriodOption() {
		final var correctionPeriodKey = "ping-wheel.settings.correctionPeriod";

		return OptionUtils.ofFloat(
			correctionPeriodKey,
			0.1f, 5.0f, 0.1f,
			(value) -> Component.translatable(correctionPeriodKey, String.format("%.1fs", value)),
			config::getCorrectionPeriod,
			config::setCorrectionPeriod
		);
	}

	private OptionInstance<Boolean> getItemIconsVisibleOption() {
		return OptionUtils.ofBool(
			"ping-wheel.settings.itemIconVisible",
			config::isItemIconVisible,
			config::setItemIconVisible
		);
	}

	private OptionInstance<Boolean> getDirectionIndicatorVisibleOption() {
		return OptionUtils.ofBool(
			"ping-wheel.settings.directionIndicatorVisible",
			config::isDirectionIndicatorVisible,
			config::setDirectionIndicatorVisible
		);
	}

	private OptionInstance<Boolean> getNameLabelForcedOption() {
		return OptionUtils.ofBool(
			"ping-wheel.settings.nameLabelForced",
			config::isNameLabelForced,
			config::setNameLabelForced
		);
	}

	private OptionInstance<Integer> getPingSizeOption() {
		final var pingSizeKey = "ping-wheel.settings.pingSize";

		return OptionUtils.ofInt(
			pingSizeKey,
			40, 300, 10,
			(value) -> Component.translatable(pingSizeKey, String.format("%s%%", value)),
			config::getPingSize,
			config::setPingSize
		);
	}
}
