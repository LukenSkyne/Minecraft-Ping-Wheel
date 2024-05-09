package nx.pingwheel.common.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.TooltipAccessor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import nx.pingwheel.common.config.ClientConfig;
import nx.pingwheel.common.helper.LanguageUtils;
import nx.pingwheel.common.helper.OptionUtils;

import java.util.Collections;
import java.util.List;

import static nx.pingwheel.common.ClientGlobal.ConfigHandler;
import static nx.pingwheel.common.config.ClientConfig.*;

public class SettingsScreen extends Screen {

	private final ClientConfig config;

	private Screen parent;
	private OptionsList list;
	private EditBox channelTextField;

	public SettingsScreen() {
		super(LanguageUtils.settings("title").get());
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

		drawString(matrices, this.font, LanguageUtils.settings("channel").get(), this.width / 2 - 100, this.channelTextField.y - 12, 10526880);
		this.channelTextField.render(matrices, mouseX, mouseY, delta);

		super.render(matrices, mouseX, mouseY, delta);

		var tooltipLines = getHoveredButtonTooltip(this.list, mouseX, mouseY);

		if (tooltipLines.isEmpty() && (this.channelTextField.isHoveredOrFocused() && !this.channelTextField.isFocused())) {
			tooltipLines = this.font.split(LanguageUtils.settings("channel.tooltip").get(), 140);
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

	private OptionInstance<Integer> getPingVolumeOption() {
		final var text = LanguageUtils.settings("pingVolume");

		return OptionUtils.ofInt(
			text.key(),
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
		final var text = LanguageUtils.settings("pingDuration");

		return OptionUtils.ofInt(
			text.key(),
			1, MAX_PING_DURATION, 1,
			(value) -> {
				if (value >= MAX_PING_DURATION) {
					return text.get(LanguageUtils.SYMBOL_INFINITE);
				}

				return text.get(LanguageUtils.UNIT_SECONDS.get(value));
			},
			config::getPingDuration,
			config::setPingDuration
		);
	}

	private OptionInstance<Integer> getPingDistanceOption() {
		final var text = LanguageUtils.settings("pingDistance");

		return OptionUtils.ofInt(
			text.key(),
			0, MAX_PING_DISTANCE, 16,
			(value) -> {
				if (value == 0) {
					return text.get(LanguageUtils.VALUE_HIDDEN);
				} else if (value >= MAX_PING_DISTANCE) {
					return text.get(LanguageUtils.SYMBOL_INFINITE);
				}

				return text.get(LanguageUtils.UNIT_METERS.get(value));
			},
			config::getPingDistance,
			config::setPingDistance
		);
	}

	private OptionInstance<Float> getCorrectionPeriodOption() {
		final var text = LanguageUtils.settings("correctionPeriod");

		return OptionUtils.ofFloat(
			text.key(),
			0.1f, MAX_CORRECTION_PERIOD, 0.1f,
			(value) -> {
				if (value >= MAX_CORRECTION_PERIOD) {
					return text.get(LanguageUtils.SYMBOL_INFINITE);
				}

				return text.get(LanguageUtils.UNIT_SECONDS.get("%.1f".formatted(value)));
			},
			config::getCorrectionPeriod,
			config::setCorrectionPeriod
		);
	}

	private OptionInstance<Boolean> getItemIconsVisibleOption() {
		return OptionUtils.ofBool(
			LanguageUtils.settings("itemIconVisible").key(),
			config::isItemIconVisible,
			config::setItemIconVisible
		);
	}

	private OptionInstance<Boolean> getDirectionIndicatorVisibleOption() {
		return OptionUtils.ofBool(
			LanguageUtils.settings("directionIndicatorVisible").key(),
			config::isDirectionIndicatorVisible,
			config::setDirectionIndicatorVisible
		);
	}

	private OptionInstance<Boolean> getNameLabelForcedOption() {
		return OptionUtils.ofBool(
			LanguageUtils.settings("nameLabelForced").key(),
			config::isNameLabelForced,
			config::setNameLabelForced
		);
	}

	private OptionInstance<Integer> getPingSizeOption() {
		final var text = LanguageUtils.settings("pingSize");

		return OptionUtils.ofInt(
			text.key(),
			40, 300, 10,
			(value) -> text.get(LanguageUtils.UNIT_PERCENT.get(value)),
			config::getPingSize,
			config::setPingSize
		);
	}
}
