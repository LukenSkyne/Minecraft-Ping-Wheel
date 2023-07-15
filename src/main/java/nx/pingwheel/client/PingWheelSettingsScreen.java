package nx.pingwheel.client;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.CyclingOption;
import net.minecraft.client.option.DoubleOption;
import net.minecraft.client.util.OrderableTooltip;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import nx.pingwheel.client.config.Config;

import java.util.List;
import java.util.Optional;

import static nx.pingwheel.client.PingWheelClient.ConfigHandler;

public class PingWheelSettingsScreen extends Screen {

	final private Config config;

	private Screen parent;
	private ButtonListWidget list;
	private TextFieldWidget channelTextField;

	public PingWheelSettingsScreen() {
		super(new TranslatableText("ping-wheel.settings.title"));
		this.config = ConfigHandler.getConfig();
	}

	public PingWheelSettingsScreen(Screen parent) {
		this();
		this.parent = parent;
	}

	public void tick() {
		this.channelTextField.tick();
	}

	protected void init() {
		this.list = new ButtonListWidget(this.client, this.width, this.height, 32, this.height - 32, 25);

		var pingVolumeOption = new DoubleOption(
				"ping-wheel.settings.pingVolume",
				0, 100, 1,
				(gameOptions) -> (double)config.getPingVolume(),
				(gameOptions, pingVolume) -> config.setPingVolume(pingVolume.intValue()),
				(gameOptions, option) -> {
					if (config.getPingVolume() == 0) {
						return new TranslatableText("ping-wheel.settings.pingVolume", ScreenTexts.OFF);
					}

					return new TranslatableText("ping-wheel.settings.pingVolume", String.format("%s%%", config.getPingVolume()));
				}
		);

		var pingDistanceOption = new DoubleOption(
				"ping-wheel.settings.pingDistance",
				0, 2048, 16,
				(gameOptions) -> (double)config.getPingDistance(),
				(gameOptions, pingDistance) -> config.setPingDistance(pingDistance.intValue()),
				(gameOptions, option) -> {
					var pingDistance = config.getPingDistance();

					if (pingDistance == 0) {
						return new TranslatableText("ping-wheel.settings.pingDistance", new TranslatableText("ping-wheel.settings.pingDistance.hidden"));
					} else if (pingDistance == 2048) {
						return new TranslatableText("ping-wheel.settings.pingDistance", new TranslatableText("ping-wheel.settings.pingDistance.unlimited"));
					}

					return new TranslatableText("ping-wheel.settings.pingDistance", String.format("%sm", pingDistance));
				}
		);

		this.list.addOptionEntry(pingVolumeOption, pingDistanceOption);

		var itemIconsVisibleOption = CyclingOption.create(
				"ping-wheel.settings.itemIconVisible",
				(gameOptions) -> config.isItemIconVisible(),
				(gameOptions, option, iconItemVisibility) -> config.setItemIconVisible(iconItemVisibility)
		);

		var pingDurationOption = new DoubleOption(
				"ping-wheel.settings.pingDuration",
				1, 60, 1,
				(gameOptions) -> (double)config.getPingDuration(),
				(gameOptions, pingDuration) -> config.setPingDuration(pingDuration.intValue()),
				(gameOptions, option) -> new TranslatableText("ping-wheel.settings.pingDuration", String.format("%ss", config.getPingDuration()))
		);

		this.list.addOptionEntry(itemIconsVisibleOption, pingDurationOption);

		this.channelTextField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 140, 200, 20, Text.of(""));
		this.channelTextField.setMaxLength(128);
		this.channelTextField.setText(config.getChannel());
		this.channelTextField.setChangedListener(config::setChannel);
		this.addSelectableChild(this.channelTextField);

		this.addSelectableChild(this.list);

		this.addDrawableChild(new ButtonWidget(this.width / 2 - 100, this.height - 27, 200, 20, ScreenTexts.DONE, (button) -> close()));
	}

	public void close() {
		ConfigHandler.save();

		if (parent != null && this.client != null) {
			this.client.setScreen(parent);
		} else {
			super.close();
		}
	}

	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		this.renderBackground(matrices);
		this.list.render(matrices, mouseX, mouseY, delta);
		drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 5, 16777215);

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
		Optional<ClickableWidget> optional = buttonList.getHoveredButton(mouseX, mouseY);
		return optional.isPresent() && optional.get() instanceof OrderableTooltip ? ((OrderableTooltip) optional.get()).getOrderedTooltip() : ImmutableList.of();
	}
}
