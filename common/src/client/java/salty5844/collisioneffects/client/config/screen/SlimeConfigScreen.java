package salty5844.collisioneffects.client.config.screen;

import salty5844.collisioneffects.client.config.Config;




import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SlimeConfigScreen extends Screen {

	private static final int ROW_HEIGHT = 24;
	private static final int LABEL_WIDTH = 200;
	private static final int TOGGLE_WIDTH = 138;
	private static final int TOGGLE_GAP = 8;

	
	
	
	

	private final Screen parent;

	public SlimeConfigScreen(Screen parent) {
		super(Component.literal("Slime"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		Config config = Config.getInstance();

		String titleText = "Slime";
		int titleWidth = this.font.width(titleText);
		this.addRenderableWidget(new StringWidget((this.width - titleWidth) / 2, 12, titleWidth, 20, Component.literal(titleText), this.font));
		this.addRenderableWidget(Button.builder(Component.literal("Reset"), button -> {
			config.resetSlimeDefaults();
			this.clearWidgets();
			this.init();
		}).bounds(this.width - 70, 12, 56, 20).build());

		int centerX = this.width / 2;
		int contentX = centerX - ((LABEL_WIDTH + TOGGLE_GAP + TOGGLE_WIDTH) / 2);
		int rowY = 48;

		addToggleRow(
			"Damaged by Slime Splatters",
			config.isEnabled("slime_damaged_splatters"),
			contentX,
			rowY,
			value -> config.setEnabled("slime_damaged_splatters", value)
		);
		rowY += ROW_HEIGHT;

		addToggleRow(
			"Hit Slime Splatters",
			config.isEnabled("slime_hit_splatters"),
			contentX,
			rowY,
			value -> config.setEnabled("slime_hit_splatters", value)
		);
		rowY += ROW_HEIGHT;

		addToggleRow(
			"Touch Slime Splatters",
			config.isEnabled("slime_touch_splatters"),
			contentX,
			rowY,
			value -> config.setEnabled("slime_touch_splatters", value)
		);

		this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> {
			Config.getInstance().save(Config.getConfigDir());
			this.onClose();
		}).bounds(centerX - 100, this.height - 48, 200, 20).build());
	}

	private CycleButton<Boolean> addToggleRow(String label, boolean initialValue, int rowX, int rowY, BooleanValueConsumer onChange) {
		String displayLabel = requireNonNullString(label);
		addWrappedCenteredLabel(displayLabel, rowX, rowY);
		CycleButton<Boolean> toggle = CycleButton.onOffBuilder(initialValue)
			.displayOnlyValue()
			.withTooltip(value -> Tooltip.create(Component.literal(Boolean.TRUE.equals(value) ? "Enabled" : "Disabled")))
			.create(rowX + LABEL_WIDTH + TOGGLE_GAP, rowY, TOGGLE_WIDTH, 20, Component.empty(), (button, value) -> onChange.accept(Boolean.TRUE.equals(value)));
		this.addRenderableWidget(toggle);
		return toggle;
	}

private void addWrappedCenteredLabel(String label, int rowX, int rowY) {
String displayLabel = requireNonNullString(label);
if (this.font.width(displayLabel) <= LABEL_WIDTH) {
this.addRenderableWidget(new StringWidget(rowX, rowY, LABEL_WIDTH, 20, Component.literal(displayLabel), this.font));
return;
}

int wrapIndex = findCenteredWrapIndex(displayLabel, LABEL_WIDTH);
if (wrapIndex <= 0 || wrapIndex >= displayLabel.length() - 1) {
this.addRenderableWidget(new StringWidget(rowX, rowY, LABEL_WIDTH, 20, Component.literal(displayLabel), this.font));
return;
}

String firstLine = requireNonNullString(displayLabel.substring(0, wrapIndex).trim());
String secondLine = requireNonNullString(displayLabel.substring(wrapIndex + 1).trim());
int firstWidth = this.font.width(firstLine);
int secondWidth = this.font.width(secondLine);
this.addRenderableWidget(new StringWidget(rowX + Math.max(0, (LABEL_WIDTH - firstWidth) / 2), rowY, firstWidth, 10, Component.literal(firstLine), this.font));
this.addRenderableWidget(new StringWidget(rowX + Math.max(0, (LABEL_WIDTH - secondWidth) / 2), rowY + 10, secondWidth, 10, Component.literal(secondLine), this.font));
}

private int findCenteredWrapIndex(String text, int maxWidth) {
int bestIndex = -1;
int bestDelta = Integer.MAX_VALUE;
for (int i = 1; i < text.length() - 1; i++) {
if (text.charAt(i) != ' ') {
continue;
}
String left = requireNonNullString(text.substring(0, i).trim());
String right = requireNonNullString(text.substring(i + 1).trim());
if (left.isEmpty() || right.isEmpty()) {
continue;
}
if (this.font.width(left) <= maxWidth && this.font.width(right) <= maxWidth) {
int delta = Math.abs(left.length() - right.length());
if (delta < bestDelta) {
bestDelta = delta;
bestIndex = i;
}
}
}
return bestIndex;
}
	@Override
	public void onClose() {
		if (this.minecraft != null && parent != null) {
			this.minecraft.setScreen(parent);
			return;
		}
		super.onClose();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	private static String requireNonNullString(String value) {
		if (value == null) {
			return "";
		}
		return value;
	}

	@FunctionalInterface
	private interface BooleanValueConsumer {
		void accept(boolean value);
	}
}



