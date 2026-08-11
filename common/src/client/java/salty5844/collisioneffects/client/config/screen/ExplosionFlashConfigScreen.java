package salty5844.collisioneffects.client.config.screen;

import salty5844.collisioneffects.client.config.Config;


import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;


import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class ExplosionFlashConfigScreen extends Screen {

	private static final int ROW_HEIGHT = 24;
	private static final int GLOBAL_LABEL_PADDING = 10;
	private static final int VALUE_BOX_WIDTH = 50;
	private static final int SLIDER_WIDTH = 138;
	private static final int TOGGLE_WIDTH = 138;
	private static final int LABEL_TO_VALUE_GAP = 4;
	private static final int VALUE_TO_SLIDER_GAP = 4;

	private final Screen parent;

	public ExplosionFlashConfigScreen(Screen parent) {
		super(Component.literal("Explosions"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		Config config = Config.getInstance();
		List<AbstractWidget> explosionFlashDependentWidgets = new ArrayList<>();
		String explosionDebrisLabel = "Explosion Debris";
		String explosionFlashLabel = "Explosion Flash";
		String explosionRegisterProximityLabel = "Explosion Register Proximity";
		int labelWidth = Math.max(
			Math.max(this.font.width(explosionDebrisLabel), this.font.width(explosionFlashLabel)),
			this.font.width(explosionRegisterProximityLabel)
		) + GLOBAL_LABEL_PADDING;
		int sliderRowWidth = labelWidth + LABEL_TO_VALUE_GAP + VALUE_BOX_WIDTH + VALUE_TO_SLIDER_GAP + SLIDER_WIDTH;
		int toggleRowWidth = labelWidth + LABEL_TO_VALUE_GAP + VALUE_BOX_WIDTH + VALUE_TO_SLIDER_GAP + TOGGLE_WIDTH;
		int contentRowWidth = Math.max(sliderRowWidth, toggleRowWidth);

		String titleText = "Explosions";
		int titleWidth = this.font.width(titleText);
		this.addRenderableWidget(new StringWidget((this.width - titleWidth) / 2, 12, titleWidth, 20, Component.literal(titleText), this.font));
		this.addRenderableWidget(Button.builder(Component.literal("Reset"), button -> {
			config.resetExplosionFlashDefaults();
			this.clearWidgets();
			this.init();
		}).bounds(this.width - 70, 12, 56, 20).build());

		int centerX = this.width / 2;
		int contentX = centerX - (contentRowWidth / 2);
		int rowY = 48;

		addToggleRow(
			explosionDebrisLabel,
			config.isEnabled("explosion_debris"),
			contentX,
			rowY,
			labelWidth,
			value -> config.setEnabled("explosion_debris", value)
		);
		rowY += ROW_HEIGHT;

		CycleButton<Boolean> explosionFlashToggle = addToggleRow(
			explosionFlashLabel,
			config.isEnabled("explosion_flash"),
			contentX,
			rowY,
			labelWidth,
			value -> {
				config.setEnabled("explosion_flash", value);
				updateDependentWidgetsActive(explosionFlashDependentWidgets, value);
			}
		);
		rowY += ROW_HEIGHT;

		addIntegerSliderRow(
			explosionRegisterProximityLabel,
			contentX,
			rowY,
			labelWidth,
			0,
			10,
			config::getExplosionRegisterProximity,
			config::setExplosionRegisterProximity,
			explosionFlashDependentWidgets
		);

		updateDependentWidgetsActive(explosionFlashDependentWidgets, explosionFlashToggle.getValue());

		this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> {
			Config.getInstance().save(Config.getConfigDir());
			this.onClose();
		}).bounds(centerX - 100, this.height - 28, 200, 20).build());
	}

	private CycleButton<Boolean> addToggleRow(String label, boolean initialValue, int rowX, int rowY, int labelWidth, BooleanValueConsumer onChange) {
		@NonNull String displayLabel = requireNonNullString(label);
		this.addRenderableWidget(new StringWidget(rowX, rowY, labelWidth, 20, Component.literal(displayLabel), this.font));
		CycleButton<Boolean> toggle = CycleButton.onOffBuilder(initialValue)
			.displayOnlyValue()
			.withTooltip(value -> Tooltip.create(Component.literal(Boolean.TRUE.equals(value) ? "Enabled" : "Disabled")))
			.create(rowX + labelWidth + LABEL_TO_VALUE_GAP + VALUE_BOX_WIDTH + VALUE_TO_SLIDER_GAP, rowY, TOGGLE_WIDTH, 20, Component.empty(), (button, value) -> onChange.accept(Boolean.TRUE.equals(value)));
		this.addRenderableWidget(toggle);
		return toggle;
	}

	private void addIntegerSliderRow(
		String label,
		int rowX,
		int rowY,
		int labelWidth,
		int min,
		int max,
		IntSupplier getter,
		IntConsumer setter,
		List<AbstractWidget> dependentWidgets
	) {
		@NonNull String displayLabel = requireNonNullString(label);
		this.addRenderableWidget(new StringWidget(rowX, rowY, labelWidth, 20, Component.literal(displayLabel), this.font));
		EditBox valueBox = new EditBox(this.font, rowX + labelWidth + LABEL_TO_VALUE_GAP, rowY, VALUE_BOX_WIDTH, 20, Component.literal(displayLabel));
		IntegerSlider slider = new IntegerSlider(
			rowX + labelWidth + LABEL_TO_VALUE_GAP + VALUE_BOX_WIDTH + VALUE_TO_SLIDER_GAP,
			rowY,
			SLIDER_WIDTH,
			20,
			min,
			max,
			getter.getAsInt(),
			value -> {
				setter.accept(value);
				valueBox.setValue(requireNonNullString(Integer.toString(value)));
			}
		);
		valueBox.setValue(requireNonNullString(Integer.toString(getter.getAsInt())));
		valueBox.setResponder(text -> {
			if (text == null || text.isBlank()) {
				return;
			}
			try {
				int parsed = Integer.parseInt(text);
				int clamped = Math.max(min, Math.min(max, parsed));
				setter.accept(clamped);
				slider.setIntValue(clamped);
				if (clamped != parsed) {
					valueBox.setValue(requireNonNullString(Integer.toString(clamped)));
				}
			} catch (NumberFormatException ignored) {
			}
		});
		this.addRenderableWidget(valueBox);
		this.addRenderableWidget(slider);
		dependentWidgets.add(valueBox);
		dependentWidgets.add(slider);
	}

	private void updateDependentWidgetsActive(List<AbstractWidget> dependentWidgets, boolean active) {
		for (AbstractWidget widget : dependentWidgets) {
			widget.active = active;
		}
	}

	@Override
	public void onClose() {
		if (this.minecraft != null && parent != null) {
			this.minecraft.setScreenAndShow(parent);
			return;
		}
		super.onClose();
	}

	@Override
	public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	private static @NonNull String requireNonNullString(@Nullable String value) {
		if (value == null) {
			return "";
		}
		return value;
	}

	private static final class IntegerSlider extends AbstractSliderButton {

		private final int min;
		private final int max;
		private final IntConsumer onValueChanged;

		private IntegerSlider(int x, int y, int width, int height, int min, int max, int initialValue, IntConsumer onValueChanged) {
			super(x, y, width, height, Component.empty(), 0.0D);
			this.min = min;
			this.max = max;
			this.onValueChanged = onValueChanged;
			setIntValue(initialValue);
		}

		private void setIntValue(int value) {
			int clamped = Math.max(min, Math.min(max, value));
			this.value = (clamped - min) / (double) (max - min);
			updateMessage();
		}

		private int getIntValue() {
			return min + (int) Math.round(this.value * (max - min));
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.literal(requireNonNullString(Integer.toString(getIntValue()))));
		}

		@Override
		protected void applyValue() {
			onValueChanged.accept(getIntValue());
		}
	}

	@FunctionalInterface
	private interface BooleanValueConsumer {
		void accept(boolean value);
	}
}
