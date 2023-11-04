package nx.pingwheel.common.screen;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.CyclingOption;
import net.minecraft.client.option.DoubleOption;
import net.minecraft.client.util.OrderableTooltip;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import nx.pingwheel.common.config.Config;

import java.util.Collections;
import java.util.List;

import static nx.pingwheel.common.ClientGlobal.ConfigHandler;

public class SettingsScreen extends Screen {

	private final Config config;

	private Screen parent;
	private ButtonListWidget list;
	private TextFieldWidget channelTextField;

	public SettingsScreen() {
		super(new TranslatableText("ping-wheel.settings.title"));
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
		this.list = new ButtonListWidget(this.client, this.width, this.height, 32, this.height - 32, 25);

		final var pingVolumeOption = getPingVolumeOption();
		final var pingDurationOption = getPingDurationOption();
		this.list.addOptionEntry(pingVolumeOption, pingDurationOption);

		final var pingDistanceOption = getPingDistanceOption();
		final var correctionPeriodOption = getCorrectionPeriodOption();
		this.list.addOptionEntry(pingDistanceOption, correctionPeriodOption);

		final var itemIconsVisibleOption = getItemIconsVisibleOption();
		final var pingSizeOption = getPingSizeOption();
		this.list.addOptionEntry(itemIconsVisibleOption, pingSizeOption);

		this.channelTextField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 140, 200, 20, Text.of(""));
		this.channelTextField.setMaxLength(128);
		this.channelTextField.setText(config.getChannel());
		this.channelTextField.setChangedListener(config::setChannel);
		this.addSelectableChild(this.channelTextField);

		this.addSelectableChild(this.list);

		this.addDrawableChild(new ButtonWidget(this.width / 2 - 100, this.height - 27, 200, 20, ScreenTexts.DONE, (button) -> close()));
	}

	@Override
	public void close() {
		ConfigHandler.save();

		if (parent != null && this.client != null) {
			this.client.setScreen(parent);
		} else {
			super.close();
		}
	}

	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		this.renderBackground(matrices);
		this.list.render(matrices, mouseX, mouseY, delta);
		drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 20, 16777215);

		drawTextWithShadow(matrices, this.textRenderer, new TranslatableText("ping-wheel.settings.channel"), this.width / 2 - 100, 128, 10526880);
		this.channelTextField.render(matrices, mouseX, mouseY, delta);

		super.render(matrices, mouseX, mouseY, delta);

		var tooltipLines = getHoveredButtonTooltip(this.list, mouseX, mouseY);

		if (tooltipLines.isEmpty() && (this.channelTextField.isHovered() && !this.channelTextField.isFocused())) {
			tooltipLines = this.textRenderer.wrapLines(new TranslatableText("ping-wheel.settings.channel.tooltip"), 140);
		}

		this.renderOrderedTooltip(matrices, tooltipLines, mouseX, mouseY);
	}

	private static List<OrderedText> getHoveredButtonTooltip(ButtonListWidget buttonList, int mouseX, int mouseY) {
		final var orderableTooltip = (OrderableTooltip)buttonList.getHoveredButton(mouseX, mouseY).orElse(null);

		if (orderableTooltip != null) {
			return orderableTooltip.getOrderedTooltip();
		}

		return Collections.emptyList();
	}

	private DoubleOption getPingVolumeOption() {
		final var pingVolumeText = "ping-wheel.settings.pingVolume";

		return new DoubleOption(
			pingVolumeText,
			0, 100, 1,
			(gameOptions) -> (double)config.getPingVolume(),
			(gameOptions, pingVolume) -> config.setPingVolume(pingVolume.intValue()),
			(gameOptions, option) -> {
				if (config.getPingVolume() == 0) {
					return new TranslatableText(pingVolumeText, ScreenTexts.OFF);
				}

				return new TranslatableText(pingVolumeText, String.format("%s%%", config.getPingVolume()));
			}
		);
	}

	private DoubleOption getPingDurationOption() {
		final var pingDurationKey = "ping-wheel.settings.pingDuration";

		return new DoubleOption(
			pingDurationKey,
			1, 60, 1,
			(gameOptions) -> (double)config.getPingDuration(),
			(gameOptions, pingDuration) -> config.setPingDuration(pingDuration.intValue()),
			(gameOptions, option) -> new TranslatableText(pingDurationKey, String.format("%ss", config.getPingDuration()))
		);
	}

	private DoubleOption getPingDistanceOption() {
		final var pingDistanceKey = "ping-wheel.settings.pingDistance";

		return new DoubleOption(
			pingDistanceKey,
			0, 2048, 16,
			(gameOptions) -> (double)config.getPingDistance(),
			(gameOptions, pingDistance) -> config.setPingDistance(pingDistance.intValue()),
			(gameOptions, option) -> {
				var pingDistance = config.getPingDistance();

				if (pingDistance == 0) {
					return new TranslatableText(pingDistanceKey, new TranslatableText(pingDistanceKey + ".hidden"));
				} else if (pingDistance == 2048) {
					return new TranslatableText(pingDistanceKey, new TranslatableText(pingDistanceKey + ".unlimited"));
				}

				return new TranslatableText(pingDistanceKey, String.format("%sm", pingDistance));
			}
		);
	}

	private DoubleOption getCorrectionPeriodOption() {
		final var correctionPeriodKey = "ping-wheel.settings.correctionPeriod";

		return new DoubleOption(
			correctionPeriodKey,
			0.1f, 5.0f, 0.1f,
			(gameOptions) -> (double) config.getCorrectionPeriod(),
			(gameOptions, correctionPeriod) -> config.setCorrectionPeriod(correctionPeriod.floatValue()),
			(gameOptions, option) -> new TranslatableText(correctionPeriodKey, String.format("%.1fs", config.getCorrectionPeriod()))
		);
	}

	private CyclingOption<Boolean> getItemIconsVisibleOption() {
		return CyclingOption.create(
			"ping-wheel.settings.itemIconVisible",
			(gameOptions) -> config.isItemIconVisible(),
			(gameOptions, option, iconItemVisibility) -> config.setItemIconVisible(iconItemVisibility)
		);
	}

	private DoubleOption getPingSizeOption() {
		final var pingSizeKey = "ping-wheel.settings.pingSize";

		return new DoubleOption(
			pingSizeKey,
			40, 300, 10,
			(gameOptions) -> (double)config.getPingSize(),
			(gameOptions, pingSize) -> config.setPingSize(pingSize.intValue()),
			(gameOptions, option) -> new TranslatableText(pingSizeKey, String.format("%s%%", config.getPingSize()))
		);
	}
}
