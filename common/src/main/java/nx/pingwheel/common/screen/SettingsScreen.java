package nx.pingwheel.common.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Option;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.TooltipAccessor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.util.FormattedCharSequence;
import nx.pingwheel.common.config.ClientConfig;
import nx.pingwheel.common.helper.OptionUtils;
import nx.pingwheel.common.compat.Component;

import java.util.Collections;
import java.util.List;

import static nx.pingwheel.common.ClientGlobal.ConfigHandler;
import static nx.pingwheel.common.config.ClientConfig.MAX_CHANNEL_LENGTH;

public class SettingsScreen extends Screen {

	private final ClientConfig config;

	private Screen parent;
	private OptionsList list;
	private EditBox channelTextField;

	public SettingsScreen() {
		super(Component.translatable("ping-wheel.settings.title"));
		this.config = ConfigHandler.getConfig();
	}

	public SettingsScreen(Screen parent) {
		this();
		this.parent = parent;
	}

	@Override
	public void tick() {
		this.channelTextField.tick();
	}

	@Override
	protected void init() {
		this.list = new OptionsList(this.minecraft, this.width, this.height, 32, this.height - 32, 25);

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

		this.channelTextField = new EditBox(this.font, this.width / 2 - 100, 160, 200, 20, Component.empty());
		this.channelTextField.setMaxLength(MAX_CHANNEL_LENGTH);
		this.channelTextField.setValue(config.getChannel());
		this.channelTextField.setResponder(config::setChannel);
		this.addWidget(this.channelTextField);

		this.addWidget(this.list);

		this.addRenderableWidget(new Button(this.width / 2 - 100, this.height - 27, 200, 20, CommonComponents.GUI_DONE, (button) -> onClose()));
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
	public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
		this.renderBackground(matrices);
		this.list.render(matrices, mouseX, mouseY, delta);
		drawCenteredString(matrices, this.font, this.title, this.width / 2, 20, 16777215);

		drawString(matrices, this.font, Component.translatable("ping-wheel.settings.channel"), this.width / 2 - 100, 148, 10526880);
		this.channelTextField.render(matrices, mouseX, mouseY, delta);

		super.render(matrices, mouseX, mouseY, delta);

		var tooltipLines = getHoveredButtonTooltip(this.list, mouseX, mouseY);

		if (tooltipLines.isEmpty() && (this.channelTextField.isHoveredOrFocused() && !this.channelTextField.isFocused())) {
			tooltipLines = this.font.split(Component.translatable("ping-wheel.settings.channel.tooltip"), 140);
		}

		this.renderTooltip(matrices, tooltipLines, mouseX, mouseY);
	}

	private static List<FormattedCharSequence> getHoveredButtonTooltip(OptionsList buttonList, int mouseX, int mouseY) {
		final var orderableTooltip = (TooltipAccessor)buttonList.getMouseOver(mouseX, mouseY).orElse(null);

		if (orderableTooltip != null) {
			return orderableTooltip.getTooltip();
		}

		return Collections.emptyList();
	}

	private Option getPingVolumeOption() {
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

	private Option getPingDurationOption() {
		final var pingDurationKey = "ping-wheel.settings.pingDuration";

		return OptionUtils.ofInt(
			pingDurationKey,
			1, 60, 1,
			(value) -> Component.translatable(pingDurationKey, String.format("%ss", config.getPingDuration())),
			config::getPingDuration,
			config::setPingDuration
		);
	}

	private Option getPingDistanceOption() {
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

	private Option getCorrectionPeriodOption() {
		final var correctionPeriodKey = "ping-wheel.settings.correctionPeriod";

		return OptionUtils.ofFloat(
			correctionPeriodKey,
			0.1f, 5.0f, 0.1f,
			(value) -> Component.translatable(correctionPeriodKey, String.format("%.1fs", value)),
			config::getCorrectionPeriod,
			config::setCorrectionPeriod
		);
	}

	private Option getItemIconsVisibleOption() {
		return OptionUtils.ofBool(
			"ping-wheel.settings.itemIconVisible",
			config::isItemIconVisible,
			config::setItemIconVisible
		);
	}

	private Option getDirectionIndicatorVisibleOption() {
		return OptionUtils.ofBool(
			"ping-wheel.settings.directionIndicatorVisible",
			config::isDirectionIndicatorVisible,
			config::setDirectionIndicatorVisible
		);
	}

	private Option getNameLabelForcedOption() {
		return OptionUtils.ofBool(
			"ping-wheel.settings.nameLabelForced",
			config::isNameLabelForced,
			config::setNameLabelForced
		);
	}

	private Option getPingSizeOption() {
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
