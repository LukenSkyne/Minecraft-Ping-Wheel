package nx.pingwheel.client;

import com.mojang.serialization.Codec;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.OptionListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class PingWheelSettingsScreen extends Screen {

	final private PingWheelConfigHandler configHandler;
	final private Config config;

	private Screen parent;
	private OptionListWidget list;
	private TextFieldWidget channelTextField;

	public PingWheelSettingsScreen() {
		super(Text.translatable("ping-wheel.settings.title"));
		this.configHandler = PingWheelConfigHandler.getInstance();
		this.config = configHandler.getConfig();
	}

	public PingWheelSettingsScreen(Screen parent) {
		this();
		this.parent = parent;
	}

	public void tick() {
		this.channelTextField.tick();

		if (this.channelTextField.isFocused() && this.getFocused() != this.channelTextField) {
			this.setFocused(this.channelTextField);
		}
	}

	protected void init() {
		this.list = new OptionListWidget(this.client, this.width, this.height, 32, this.height - 32, 25);

		var pingVolumeOption = new SimpleOption<>(
				"ping-wheel.settings.pingVolume",
				SimpleOption.emptyTooltip(),
				(optionText, value) -> {
					if (value == 0) {
						return Text.translatable("ping-wheel.settings.pingVolume", ScreenTexts.OFF);
					}

					return Text.translatable("ping-wheel.settings.pingVolume", String.format("%s%%", value));
				},
				(new SimpleOption.ValidatingIntSliderCallbacks(0, 100)),
				Codec.intRange(0, 100),
				config.getPingVolume(),
				config::setPingVolume
		);

		var pingDistanceOption = new SimpleOption<>(
				"ping-wheel.settings.pingDistance",
				SimpleOption.emptyTooltip(),
				(optionText, value) -> {
					if (value == 0) {
						return Text.translatable("ping-wheel.settings.pingDistance", Text.translatable("ping-wheel.settings.pingDistance.hidden"));
					} else if (value == 2048) {
						return Text.translatable("ping-wheel.settings.pingDistance", Text.translatable("ping-wheel.settings.pingDistance.unlimited"));
					}

					return Text.translatable("ping-wheel.settings.pingDistance", String.format("%sm", value));
				},
				(new SimpleOption.ValidatingIntSliderCallbacks(0, 128))
						.withModifier((value) -> value * 16, (value) -> value / 16),
				Codec.intRange(0, 2048),
				config.getPingDistance(),
				config::setPingDistance
		);

		this.list.addOptionEntry(pingVolumeOption, pingDistanceOption);

		var itemIconsVisibleOption = SimpleOption.ofBoolean(
				"ping-wheel.settings.itemIconVisible",
				config.isItemIconVisible(),
				config::setItemIconVisible
		);

		var pingDurationOption = new SimpleOption<>(
				"ping-wheel.settings.pingDuration",
				SimpleOption.emptyTooltip(),
				(optionText, value) -> Text.translatable("ping-wheel.settings.pingDuration", String.format("%ss", value)),
				(new SimpleOption.ValidatingIntSliderCallbacks(1, 60)),
				Codec.intRange(1, 60),
				config.getPingDuration(),
				config::setPingDuration
		);

		this.list.addOptionEntry(itemIconsVisibleOption, pingDurationOption);

		this.channelTextField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 140, 200, 20, Text.empty());
		this.channelTextField.setMaxLength(128);
		this.channelTextField.setText(config.getChannel());
		this.channelTextField.setChangedListener(config::setChannel);
		this.addSelectableChild(this.channelTextField);

		this.addSelectableChild(this.list);

		this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, (button) -> close())
				.position(this.width / 2 - 100, this.height - 27)
				.size(200, 20)
				.build());
	}

	public void close() {
		configHandler.save();

		if (parent != null && this.client != null) {
			this.client.setScreen(parent);
		} else {
			super.close();
		}
	}

	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		this.renderBackground(matrices);
		this.list.render(matrices, mouseX, mouseY, delta);
		drawCenteredTextWithShadow(matrices, this.textRenderer, this.title, this.width / 2, 5, 16777215);

		drawTextWithShadow(matrices, this.textRenderer, Text.translatable("ping-wheel.settings.channel"), this.width / 2 - 100, 128, 10526880);
		this.channelTextField.render(matrices, mouseX, mouseY, delta);

		super.render(matrices, mouseX, mouseY, delta);

		if ((this.channelTextField.isHovered() && !this.channelTextField.isFocused())) {
			this.renderOrderedTooltip(matrices, this.textRenderer.wrapLines(Text.translatable("ping-wheel.settings.channel.tooltip"), 140), mouseX, mouseY);
		}
	}
}
