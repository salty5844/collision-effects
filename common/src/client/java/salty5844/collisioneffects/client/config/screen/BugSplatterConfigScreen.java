package salty5844.collisioneffects.client.config.screen;

import salty5844.collisioneffects.client.config.Config;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
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
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class BugSplatterConfigScreen extends Screen {

	private static final int ROW_HEIGHT = 24;
	private static final int GLOBAL_LABEL_PADDING = 10;
	private static final int VALUE_BOX_WIDTH = 50;
	private static final int SLIDER_WIDTH = 138;
	private static final int TOGGLE_WIDTH = 138;
	private static final int LABEL_TO_VALUE_GAP = 4;
	private static final int VALUE_TO_SLIDER_GAP = 4;

	private final Screen parent;

	public BugSplatterConfigScreen(Screen parent) {
		super(Component.literal("Bugs"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		Config config = Config.getInstance();
		List<AbstractWidget> dependentWidgets = new ArrayList<>();
		String bugSplattersLabel = "Bug Splatters";
		String regularGroundProximityLabel = "Regular Ground Proximity";
		String closeGroundProximityLabel = "Close Ground Proximity";
		String jungleMultiplierLabel = "Jungle Multiplier";
		String swampMultiplierLabel = "Swamp Multiplier";
		int labelWidth = Math.max(
			Math.max(this.font.width(bugSplattersLabel), this.font.width(regularGroundProximityLabel)),
			Math.max(this.font.width(closeGroundProximityLabel), Math.max(this.font.width(jungleMultiplierLabel), this.font.width(swampMultiplierLabel)))
		) + GLOBAL_LABEL_PADDING;
		int sliderRowWidth = labelWidth + LABEL_TO_VALUE_GAP + VALUE_BOX_WIDTH + VALUE_TO_SLIDER_GAP + SLIDER_WIDTH;
		int toggleRowWidth = labelWidth + LABEL_TO_VALUE_GAP + VALUE_BOX_WIDTH + VALUE_TO_SLIDER_GAP + TOGGLE_WIDTH;
		int contentRowWidth = Math.max(sliderRowWidth, toggleRowWidth);

		String titleText = "Bugs";
		int titleWidth = this.font.width(titleText);
		this.addRenderableWidget(new StringWidget((this.width - titleWidth) / 2, 12, titleWidth, 20, Component.literal(titleText), this.font));
		this.addRenderableWidget(Button.builder(Component.literal("Reset"), button -> {
			config.resetBugSplatterDefaults();
			this.clearWidgets();
			this.init();
		}).bounds(this.width - 70, 12, 56, 20).build());

		int centerX = this.width / 2;
		int contentX = centerX - (contentRowWidth / 2);
		int rowY = 48;

		CycleButton<Boolean> bugSplattersToggle = addToggleRow(
			bugSplattersLabel,
			config.isEnabled("bug_splatter"),
			contentX,
			rowY,
			labelWidth,
			value -> {
				config.setEnabled("bug_splatter", value);
				updateDependentWidgetsActive(dependentWidgets, value);
			}
		);
		rowY += ROW_HEIGHT + 4;

		addIntegerSliderRow(
			regularGroundProximityLabel,
			contentX,
			rowY,
			labelWidth,
			1,
			100,
			config::getRegularGroundProximity,
			config::setRegularGroundProximity,
			dependentWidgets
		);
		rowY += ROW_HEIGHT;

		addIntegerSliderRow(
			closeGroundProximityLabel,
			contentX,
			rowY,
			labelWidth,
			1,
			100,
			config::getDoubleGroundProximity,
			config::setDoubleGroundProximity,
			dependentWidgets
		);
		rowY += ROW_HEIGHT;

		addDecimalSliderRow(
			jungleMultiplierLabel,
			contentX,
			rowY,
			labelWidth,
			0.0D,
			10.0D,
			1,
			config::getJungleMultiplier,
			config::setJungleMultiplier,
			dependentWidgets
		);
		rowY += ROW_HEIGHT;

		addDecimalSliderRow(
			swampMultiplierLabel,
			contentX,
			rowY,
			labelWidth,
			0.0D,
			10.0D,
			1,
			config::getSwampMultiplier,
			config::setSwampMultiplier,
			dependentWidgets
		);

		updateDependentWidgetsActive(dependentWidgets, bugSplattersToggle.getValue());

		this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> {
			Config.getInstance().save(Config.getConfigDir());
			this.onClose();
		}).bounds(centerX - 100, this.height - 28, 200, 20).build());
	}

	@Override
	public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean captured) {
		double mouseX = event.x();
		double mouseY = event.y();
		if (mouseX < 8 || mouseX > this.width - 8 || mouseY < 8 || mouseY > this.height - 8) {
			return true;
		}
		return super.mouseClicked(event, captured);
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

	private void addDecimalSliderRow(
		String label,
		int rowX,
		int rowY,
		int labelWidth,
		double min,
		double max,
		int precision,
		DoubleSupplier getter,
		DoubleConsumer setter,
		List<AbstractWidget> dependentWidgets
	) {
		@NonNull String displayLabel = requireNonNullString(label);
		this.addRenderableWidget(new StringWidget(rowX, rowY, labelWidth, 20, Component.literal(displayLabel), this.font));
		EditBox valueBox = new EditBox(this.font, rowX + labelWidth + LABEL_TO_VALUE_GAP, rowY, VALUE_BOX_WIDTH, 20, Component.literal(displayLabel));
		DecimalSlider slider = new DecimalSlider(
			rowX + labelWidth + LABEL_TO_VALUE_GAP + VALUE_BOX_WIDTH + VALUE_TO_SLIDER_GAP,
			rowY,
			SLIDER_WIDTH,
			20,
			min,
			max,
			precision,
			getter.getAsDouble(),
			value -> {
				setter.accept(value);
				valueBox.setValue(formatDecimal(value, precision));
			}
		);
		valueBox.setValue(formatDecimal(getter.getAsDouble(), precision));
		valueBox.setResponder(text -> {
			if (text == null || text.isBlank()) {
				return;
			}
			try {
				double parsed = Double.parseDouble(text);
				double clamped = Math.max(min, Math.min(max, parsed));
				double normalized = roundToPrecision(clamped, precision);
				setter.accept(normalized);
				slider.setDoubleValue(normalized);
				if (Math.abs(parsed - normalized) > 0.0000001D) {
					valueBox.setValue(formatDecimal(normalized, precision));
				}
			} catch (NumberFormatException ignored) {
			}
		});
		this.addRenderableWidget(valueBox);
		this.addRenderableWidget(slider);
		dependentWidgets.add(valueBox);
		dependentWidgets.add(slider);
	}

	private void updateDependentWidgetsActive(List<AbstractWidget> widgets, boolean active) {
		for (AbstractWidget widget : widgets) {
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

	private static double roundToPrecision(double value, int precision) {
		double scale = Math.pow(10.0D, precision);
		return Math.round(value * scale) / scale;
	}

	private static @NonNull String formatDecimal(double value, int precision) {
		return requireNonNullString(String.format(Locale.ROOT, "%1$." + precision + "f", value));
	}

	@FunctionalInterface
	private interface BooleanValueConsumer {
		void accept(boolean value);
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
			this.setIntValue(initialValue);
		}

		private void setIntValue(int value) {
			this.value = (double) (Math.max(min, Math.min(max, value)) - min) / (double) (max - min);
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			this.setMessage(Component.literal(requireNonNullString(Integer.toString(getIntValue()))));
		}

		@Override
		protected void applyValue() {
			onValueChanged.accept(getIntValue());
		}

		private int getIntValue() {
			return min + (int) Math.round(this.value * (max - min));
		}
	}

	private static final class DecimalSlider extends AbstractSliderButton {
		private final double min;
		private final double max;
		private final int precision;
		private final DoubleConsumer onValueChanged;

		private DecimalSlider(int x, int y, int width, int height, double min, double max, int precision, double initialValue, DoubleConsumer onValueChanged) {
			super(x, y, width, height, Component.empty(), 0.0D);
			this.min = min;
			this.max = max;
			this.precision = precision;
			this.onValueChanged = onValueChanged;
			this.setDoubleValue(initialValue);
		}

		private void setDoubleValue(double value) {
			double clamped = Math.max(min, Math.min(max, value));
			this.value = (clamped - min) / (max - min);
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			this.setMessage(Component.literal(formatDecimal(getDoubleValue(), precision)));
		}

		@Override
		protected void applyValue() {
			onValueChanged.accept(getDoubleValue());
		}

		private double getDoubleValue() {
			double raw = min + (this.value * (max - min));
			return roundToPrecision(raw, precision);
		}
	}
}