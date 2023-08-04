package nx.pingwheel.client;

import com.mojang.serialization.Codec;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.OptionListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import nx.pingwheel.client.config.Config;

import static nx.pingwheel.client.PingWheelClient.ConfigHandler;

public class PingWheelSettingsScreen extends Screen {

	final private Config config;

	private Screen parent;
	private OptionListWidget list;
	private TextFieldWidget channelTextField;
	private ButtonWidget iconButtonWidget;

	public PingWheelSettingsScreen() {
		super(Text.translatable("ping-wheel.settings.title"));
		this.config = ConfigHandler.getConfig();
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

		var pingDurationOption = new SimpleOption<>(
			"ping-wheel.settings.pingDuration",
			SimpleOption.emptyTooltip(),
			(optionText, value) -> Text.translatable("ping-wheel.settings.pingDuration", String.format("%ss", value)),
			(new SimpleOption.ValidatingIntSliderCallbacks(1, 60)),
			Codec.intRange(1, 60),
			config.getPingDuration(),
			config::setPingDuration
		);

		this.list.addOptionEntry(pingVolumeOption, pingDurationOption);

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

		var correctionPeriodOption = new SimpleOption<>(
			"ping-wheel.settings.correctionPeriod",
			SimpleOption.emptyTooltip(),
			(optionText, value) -> Text.translatable("ping-wheel.settings.correctionPeriod", String.format("%.1fs", value * 0.1f)),
			(new SimpleOption.ValidatingIntSliderCallbacks(1, 50)),
			Codec.intRange(1, 50),
			(int)(config.getCorrectionPeriod() * 10f),
			(value) -> config.setCorrectionPeriod(value * 0.1f)
		);

		this.list.addOptionEntry(pingDistanceOption, correctionPeriodOption);

		var iconSizeOption = new SimpleOption<>(
			"ping-wheel.settings.iconSize",
			SimpleOption.emptyTooltip(),
			(optionText, value) -> Text.translatable("ping-wheel.settings.iconSize", String.format("%spx", value)),
			(new SimpleOption.ValidatingIntSliderCallbacks(1, 32)),
			Codec.intRange(1, 32),
			config.getIconSize(),
			config::setIconSize
		);

		var itemIconsVisibleOption = SimpleOption.ofBoolean(
			"ping-wheel.settings.itemIconVisible",
			config.isItemIconVisible(),
			config::setItemIconVisible
		);

		this.list.addOptionEntry(iconSizeOption, itemIconsVisibleOption);

		this.iconButtonWidget = ButtonWidget.builder(Text.translatable("ping-wheel.settings.changeIcon"), (button) -> config.getIcon().nextIcon(4))
				.dimensions(this.width / 2 - 155, 110, 150, 20)
				.build();

		this.addDrawableChild(iconButtonWidget);

		this.channelTextField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 165, 200, 20, Text.empty());
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
		ConfigHandler.save();

		if (parent != null && this.client != null) {
			this.client.setScreen(parent);
		} else {
			super.close();
		}
	}

	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		this.renderBackground(ctx);
		this.list.render(ctx, mouseX, mouseY, delta);
		ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 5, 16777215);

		MinecraftClient.getInstance().getTextureManager().bindTexture(config.getIcon().getTextureId());

		ctx.drawTextWithShadow(this.textRenderer, Text.translatable("ping-wheel.settings.channel"), this.width / 2 - 100, 150, 10526880);
		this.channelTextField.render(ctx, mouseX, mouseY, delta);

		super.render(ctx, mouseX, mouseY, delta);

		var textureSize = this.iconButtonWidget.getHeight() / 2;
		var padding = (this.iconButtonWidget.getHeight() - textureSize) / 2;

		ctx.drawTexture(
			config.getIcon().getTextureId(),
			this.iconButtonWidget.getX() + this.iconButtonWidget.getWidth() - this.iconButtonWidget.getHeight() + padding,
			this.iconButtonWidget.getY() + padding,
			0,
			0,
			0,
			textureSize,
			textureSize,
			textureSize,
			textureSize
		);

		if (this.channelTextField.isHovered() && !this.channelTextField.isFocused()) {
			ctx.drawOrderedTooltip(this.textRenderer, this.textRenderer.wrapLines(Text.translatable("ping-wheel.settings.channel.tooltip"), 140), mouseX, mouseY);
		}
	}
}
