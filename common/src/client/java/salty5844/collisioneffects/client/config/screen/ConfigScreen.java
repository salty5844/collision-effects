package salty5844.collisioneffects.client.config.screen;

import salty5844.collisioneffects.client.config.Config;


import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class ConfigScreen extends Screen {

	private static final int GLOBAL_LABEL_PADDING = 10;
	private static final int TOGGLE_WIDTH = 138;
	private static final int VALUE_BOX_WIDTH = 50;
	private static final int SLIDER_WIDTH = 138;
	private static final int HOTKEY_BUTTON_WIDTH = 104;
	private static final int HOTKEY_RESET_BUTTON_WIDTH = 82;
	private static final int HOTKEY_BUTTON_GAP = 6;
	private static final int LABEL_TO_VALUE_GAP = 4;
	private static final int VALUE_TO_SLIDER_GAP = 4;
	private static final int MENU_ROW_HEIGHT = 26;
	private static final int COLUMN_HEADER_Y = 194;
	private static final int FIRST_MENU_ROW_Y = 214;
	private static final int COLUMN_GAP = 16;
	private static final int CONTENT_TOP_Y = 36;
	private static final int FOOTER_HEIGHT = 34;
	private static final int VIEWPORT_INSET_Y = 1;
	private static final int CONTENT_BOTTOM_PADDING = 4;
	private static final int SCROLLBAR_WIDTH = 6;
	private static final int SCROLLBAR_MARGIN_RIGHT = 8;
	private static final int SCROLLBAR_MIN_HANDLE_HEIGHT = 16;
	private static final int SCROLLBAR_TRACK_COLOR = 0x66000000;
	private static final int SCROLLBAR_HANDLE_COLOR = 0xFFB0B0B0;
	private static final int SCROLLBAR_HANDLE_DRAGGING_COLOR = 0xFFD0D0D0;
	private static final int SCROLLBAR_BORDER_COLOR = 0x99000000;

	private final Screen parent;
	private final List<AbstractSliderButton> sliderWidgets = new ArrayList<>();
	private final List<net.minecraft.client.gui.components.AbstractWidget> scrollableWidgets = new ArrayList<>();
	private final Map<net.minecraft.client.gui.components.AbstractWidget, Integer> scrollBaseY = new IdentityHashMap<>();
	private int maxScroll = 0;
	private int scrollOffset = 0;
	private boolean draggingScrollbar = false;
	private int scrollbarDragOffset = 0;
	private @Nullable Button configurationHotkeyButton;
	private @Nullable Button configurationHotkeyResetButton;
	private boolean listeningForConfigurationHotkey = false;
	private boolean configurationHotkeyHasConflict = false;
	private final List<Component> configurationHotkeyConflicts = new ArrayList<>();

	public ConfigScreen(Screen parent) {
		super(Component.literal("Collision Effects Configuration"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		scrollableWidgets.clear();
		scrollBaseY.clear();
		sliderWidgets.clear();
		maxScroll = 0;

		Config config = Config.getInstance();
		String centerDeadzoneLabel = "Center Deadzone";
		String configurationHotkeyLabel = "Configuration Hotkey";
		String thirdPersonEffectsLabel = "Third Person Effects";
		String mobHitTrackingRadiusLabel = "Mob Hit Tracking Radius";
		String particleCapacityLabel = "Particle Capacity";
		String particleOpacityLabel = "Particle Opacity";
		int globalLabelWidth = Math.max(
			Math.max(this.font.width(centerDeadzoneLabel), this.font.width(configurationHotkeyLabel)),
			Math.max(Math.max(this.font.width(thirdPersonEffectsLabel), this.font.width(mobHitTrackingRadiusLabel)), Math.max(this.font.width(particleCapacityLabel), this.font.width(particleOpacityLabel)))
		) + GLOBAL_LABEL_PADDING;
		int sliderRowWidth = globalLabelWidth + LABEL_TO_VALUE_GAP + VALUE_BOX_WIDTH + VALUE_TO_SLIDER_GAP + SLIDER_WIDTH;
		int toggleRowWidth = globalLabelWidth + LABEL_TO_VALUE_GAP + VALUE_BOX_WIDTH + VALUE_TO_SLIDER_GAP + TOGGLE_WIDTH;
		int hotkeyRowWidth = globalLabelWidth + LABEL_TO_VALUE_GAP + HOTKEY_BUTTON_WIDTH + HOTKEY_BUTTON_GAP + HOTKEY_RESET_BUTTON_WIDTH;
		int globalRowsWidth = Math.max(Math.max(sliderRowWidth, toggleRowWidth), hotkeyRowWidth);
		String titleText = "Collision Effects Configuration";
		int titleWidth = this.font.width(titleText);
		this.addRenderableWidget(new StringWidget((this.width - titleWidth) / 2, 12, titleWidth, 20, Component.literal(titleText), this.font));
		this.addRenderableWidget(Button.builder(Component.literal("Reset"), button -> {
			if (this.minecraft != null) {
				this.minecraft.setScreenAndShow(new ResetConfirmScreen(this));
			}
		}).bounds(this.width - 70, 12, 56, 20).build());

		int centerX = this.width / 2;
		int thirdPersonLabelX = centerX - (globalRowsWidth / 2);
		addCenterDeadzoneSliderRow(config, thirdPersonLabelX, 42, globalLabelWidth, centerDeadzoneLabel);
		addConfigurationHotkeyRow(config, thirdPersonLabelX, 66, globalLabelWidth, configurationHotkeyLabel);

		addScrollableWidget(new StringWidget(thirdPersonLabelX, 90, globalLabelWidth, 20, Component.literal(thirdPersonEffectsLabel), this.font), 90);
		CycleButton<Boolean> thirdPersonEffectsToggle = CycleButton.onOffBuilder(config.isThirdPersonEffectsEnabled())
			.displayOnlyValue()
			.withTooltip(value -> Tooltip.create(Component.literal(Boolean.TRUE.equals(value) ? "Enabled" : "Disabled")))
			.create(thirdPersonLabelX + globalLabelWidth + LABEL_TO_VALUE_GAP + VALUE_BOX_WIDTH + VALUE_TO_SLIDER_GAP, 90, TOGGLE_WIDTH, 20, Component.empty(), (button, value) -> config.setThirdPersonEffectsEnabled(Boolean.TRUE.equals(value)));
		addScrollableWidget(thirdPersonEffectsToggle, 90);

		addMobHitTrackingRadiusSliderRow(config, thirdPersonLabelX, 114, globalLabelWidth, mobHitTrackingRadiusLabel);
		addParticleCapacitySliderRow(config, thirdPersonLabelX, 138, globalLabelWidth, particleCapacityLabel);
		addParticleOpacitySliderRow(config, thirdPersonLabelX, 162, globalLabelWidth, particleOpacityLabel);

		int buttonWidth = 132;
		int leftColumnX = centerX - buttonWidth - (COLUMN_GAP / 2);
		int rightColumnX = centerX + (COLUMN_GAP / 2);
		String environmentHeader = "Environment";
		String mobsHeader = "Mobs";
		int environmentHeaderWidth = this.font.width(environmentHeader);
		int mobsHeaderWidth = this.font.width(mobsHeader);
		addScrollableWidget(new StringWidget(
			leftColumnX + (buttonWidth - environmentHeaderWidth) / 2,
			COLUMN_HEADER_Y,
			environmentHeaderWidth,
			20,
			Component.literal(environmentHeader),
			this.font
		), COLUMN_HEADER_Y);
		addScrollableWidget(new StringWidget(
			rightColumnX + (buttonWidth - mobsHeaderWidth) / 2,
			COLUMN_HEADER_Y,
			mobsHeaderWidth,
			20,
			Component.literal(mobsHeader),
			this.font
		), COLUMN_HEADER_Y);

		int leftRowY = FIRST_MENU_ROW_Y;
		int rightRowY = FIRST_MENU_ROW_Y;

		addScrollableWidget(Button.builder(Component.literal("Bugs"), button -> {
			if (this.minecraft != null) {
				this.minecraft.setScreenAndShow(new BugSplatterConfigScreen(this));
			}
		}).bounds(leftColumnX, leftRowY, buttonWidth, 20).build(), leftRowY);
		leftRowY += MENU_ROW_HEIGHT;

		addScrollableWidget(Button.builder(Component.literal("Damage"), button -> {
			if (this.minecraft != null) {
				this.minecraft.setScreenAndShow(new DamageTintConfigScreen(this));
			}
		}).bounds(leftColumnX, leftRowY, buttonWidth, 20).build(), leftRowY);
		leftRowY += MENU_ROW_HEIGHT;

		addScrollableWidget(Button.builder(Component.literal("Explosions"), button -> {
			if (this.minecraft != null) {
				this.minecraft.setScreenAndShow(new ExplosionFlashConfigScreen(this));
			}
		}).bounds(leftColumnX, leftRowY, buttonWidth, 20).build(), leftRowY);
		leftRowY += MENU_ROW_HEIGHT;

		addSingleOptionToggleButton("Lava Splatters", "lava_splatters", leftColumnX, leftRowY, buttonWidth);
		leftRowY += MENU_ROW_HEIGHT;

		addScrollableWidget(Button.builder(Component.literal("Rain/Water"), button -> {
			if (this.minecraft != null) {
				this.minecraft.setScreenAndShow(new WaterRainConfigScreen(this));
			}
		}).bounds(leftColumnX, leftRowY, buttonWidth, 20).build(), leftRowY);
		leftRowY += MENU_ROW_HEIGHT;

		addScrollableWidget(Button.builder(Component.literal("Snow"), button -> {
			if (this.minecraft != null) {
				this.minecraft.setScreenAndShow(new SnowConfigScreen(this));
			}
		}).bounds(leftColumnX, leftRowY, buttonWidth, 20).build(), leftRowY);

		addScrollableWidget(Button.builder(Component.literal("Bee"), button -> {
			if (this.minecraft != null) {
				this.minecraft.setScreenAndShow(new BeePollenConfigScreen(this));
			}
		}).bounds(rightColumnX, rightRowY, buttonWidth, 20).build(), rightRowY);
		rightRowY += MENU_ROW_HEIGHT;

		addScrollableWidget(Button.builder(Component.literal("Chicken"), button -> {
			if (this.minecraft != null) {
				this.minecraft.setScreenAndShow(new ChickenFeathersConfigScreen(this));
			}
		}).bounds(rightColumnX, rightRowY, buttonWidth, 20).build(), rightRowY);
		rightRowY += MENU_ROW_HEIGHT;

		addSingleOptionToggleButton("Llama Spit", "llama_spit", rightColumnX, rightRowY, buttonWidth);
		rightRowY += MENU_ROW_HEIGHT;

		addScrollableWidget(Button.builder(Component.literal("Magma Cube"), button -> {
			if (this.minecraft != null) {
				this.minecraft.setScreenAndShow(new MagmaConfigScreen(this));
			}
		}).bounds(rightColumnX, rightRowY, buttonWidth, 20).build(), rightRowY);
		rightRowY += MENU_ROW_HEIGHT;

		addScrollableWidget(Button.builder(Component.literal("Slime"), button -> {
			if (this.minecraft != null) {
				this.minecraft.setScreenAndShow(new SlimeConfigScreen(this));
			}
		}).bounds(rightColumnX, rightRowY, buttonWidth, 20).build(), rightRowY);
		rightRowY += MENU_ROW_HEIGHT;

		addSingleOptionToggleButton("Spider Splatters", "spider_splatters", rightColumnX, rightRowY, buttonWidth);
		rightRowY += MENU_ROW_HEIGHT;

		addSingleOptionToggleButton("Squid Ink", "squid_ink", rightColumnX, rightRowY, buttonWidth);
		rightRowY += MENU_ROW_HEIGHT;

		addScrollableWidget(Button.builder(Component.literal("Sulfur Cube"), button -> {
			if (this.minecraft != null) {
				this.minecraft.setScreenAndShow(new SulfurConfigScreen(this));
			}
		}).bounds(rightColumnX, rightRowY, buttonWidth, 20).build(), rightRowY);
		rightRowY += MENU_ROW_HEIGHT;

		addSingleOptionToggleButton("Wither Vignette", "wither_vingette", rightColumnX, rightRowY, buttonWidth);
		rightRowY += MENU_ROW_HEIGHT;

		updateScrollBounds();
		applyScrollAndVisibility();
		this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> {
			Config.getInstance().save(Config.getConfigDir());
			this.onClose();
		}).bounds(centerX - 100, this.height - 28, 200, 20).build());
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (maxScroll > 0 && scrollY != 0.0D) {
			int scrollStep = Math.max(1, MENU_ROW_HEIGHT / 2);
			scrollBy(scrollY > 0.0D ? -scrollStep : scrollStep);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean captured) {
		if (listeningForConfigurationHotkey && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			listeningForConfigurationHotkey = false;
			refreshConfigurationHotkeyButtons(Config.getInstance());
		}
		double mouseX = event.x();
		double mouseY = event.y();
		if (event.button() == 0 && maxScroll > 0 && isInScrollbarTrack(mouseX, mouseY)) {
			int handleY = getScrollbarHandleY();
			int handleHeight = getScrollbarHandleHeight();
			if (mouseY >= handleY && mouseY < handleY + handleHeight) {
				draggingScrollbar = true;
				scrollbarDragOffset = (int) mouseY - handleY;
			} else {
				draggingScrollbar = true;
				scrollbarDragOffset = handleHeight / 2;
				setScrollFromHandleY((int) mouseY - scrollbarDragOffset);
			}
			return true;
		}
		return super.mouseClicked(event, captured);
	}

	@Override
	public boolean keyPressed(@NonNull KeyEvent event) {
		if (listeningForConfigurationHotkey) {
			InputConstants.Key key = InputConstants.getKey(event);
			if (key.getValue() != InputConstants.KEY_ESCAPE) {
				Config.getInstance().setConfigurationHotkey(key.getValue());
			}
			listeningForConfigurationHotkey = false;
			refreshConfigurationHotkeyButtons(Config.getInstance());
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
		if (event.button() == 0 && draggingScrollbar) {
			double mouseY = event.y();
			setScrollFromHandleY((int) mouseY - scrollbarDragOffset);
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(@NonNull MouseButtonEvent event) {
		if (event.button() == 0 && draggingScrollbar) {
			draggingScrollbar = false;
			return true;
		}
		return super.mouseReleased(event);
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
		renderScrollableWidgets(graphics, mouseX, mouseY, partialTick);
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		renderScrollbar(graphics);
	}

	private void addCenterDeadzoneSliderRow(Config config, int rowX, int rowY, int labelWidth, String label) {
		@NonNull String displayLabel = requireNonNullString(label);
		addScrollableWidget(new StringWidget(rowX, rowY, labelWidth, 20, Component.literal(displayLabel), this.font), rowY);
		EditBox valueBox = new EditBox(this.font, rowX + labelWidth + LABEL_TO_VALUE_GAP, rowY, VALUE_BOX_WIDTH, 20, Component.literal(displayLabel));
		IntegerSlider slider = new IntegerSlider(
			rowX + labelWidth + LABEL_TO_VALUE_GAP + VALUE_BOX_WIDTH + VALUE_TO_SLIDER_GAP,
			rowY,
			SLIDER_WIDTH,
			20,
			0,
			100,
			config.getCenterDeadzone(),
			value -> {
				config.setCenterDeadzone(value);
				valueBox.setValue(requireNonNullString(Integer.toString(value)));
			}
		);

		valueBox.setValue(requireNonNullString(Integer.toString(config.getCenterDeadzone())));
		valueBox.setResponder(text -> {
			if (text == null || text.isBlank()) {
				return;
			}
			try {
				int parsed = Integer.parseInt(text);
				int clamped = Math.max(0, Math.min(100, parsed));
				config.setCenterDeadzone(clamped);
				slider.setIntValue(clamped);
				if (clamped != parsed) {
					valueBox.setValue(requireNonNullString(Integer.toString(clamped)));
				}
			} catch (NumberFormatException ignored) {
			}
		});

		addScrollableWidget(valueBox, rowY);
		addScrollableWidget(slider, rowY);
	}

	private void addMobHitTrackingRadiusSliderRow(Config config, int rowX, int rowY, int labelWidth, String label) {
		@NonNull String displayLabel = requireNonNullString(label);
		addScrollableWidget(new StringWidget(rowX, rowY, labelWidth, 20, Component.literal(displayLabel), this.font), rowY);
		EditBox valueBox = new EditBox(this.font, rowX + labelWidth + LABEL_TO_VALUE_GAP, rowY, VALUE_BOX_WIDTH, 20, Component.literal(displayLabel));
		IntegerSlider slider = new IntegerSlider(
			rowX + labelWidth + LABEL_TO_VALUE_GAP + VALUE_BOX_WIDTH + VALUE_TO_SLIDER_GAP,
			rowY,
			SLIDER_WIDTH,
			20,
			0,
			5,
			config.getMobHitTrackingRadius(),
			value -> {
				config.setMobHitTrackingRadius(value);
				valueBox.setValue(requireNonNullString(Integer.toString(value)));
			}
		);

		valueBox.setValue(requireNonNullString(Integer.toString(config.getMobHitTrackingRadius())));
		valueBox.setResponder(text -> {
			if (text == null || text.isBlank()) {
				return;
			}
			try {
				int parsed = Integer.parseInt(text);
				int clamped = Math.max(0, Math.min(5, parsed));
				config.setMobHitTrackingRadius(clamped);
				slider.setIntValue(clamped);
				if (clamped != parsed) {
					valueBox.setValue(requireNonNullString(Integer.toString(clamped)));
				}
			} catch (NumberFormatException ignored) {
			}
		});

		addScrollableWidget(valueBox, rowY);
		addScrollableWidget(slider, rowY);
	}

	private void addParticleCapacitySliderRow(Config config, int rowX, int rowY, int labelWidth, String label) {
		@NonNull String displayLabel = requireNonNullString(label);
		addScrollableWidget(new StringWidget(rowX, rowY, labelWidth, 20, Component.literal(displayLabel), this.font), rowY);
		EditBox valueBox = new EditBox(this.font, rowX + labelWidth + LABEL_TO_VALUE_GAP, rowY, VALUE_BOX_WIDTH, 20, Component.literal(displayLabel));
		IntegerSlider slider = new IntegerSlider(
			rowX + labelWidth + LABEL_TO_VALUE_GAP + VALUE_BOX_WIDTH + VALUE_TO_SLIDER_GAP,
			rowY,
			SLIDER_WIDTH,
			20,
			0,
			200,
			config.getParticleCapacity(),
			value -> {
				config.setParticleCapacity(value);
				valueBox.setValue(requireNonNullString(Integer.toString(value)));
			}
		);

		valueBox.setValue(requireNonNullString(Integer.toString(config.getParticleCapacity())));
		valueBox.setResponder(text -> {
			if (text == null || text.isBlank()) {
				return;
			}
			try {
				int parsed = Integer.parseInt(text);
				int clamped = Math.max(0, Math.min(200, parsed));
				config.setParticleCapacity(clamped);
				slider.setIntValue(clamped);
				if (clamped != parsed) {
					valueBox.setValue(requireNonNullString(Integer.toString(clamped)));
				}
			} catch (NumberFormatException ignored) {
			}
		});

		addScrollableWidget(valueBox, rowY);
		addScrollableWidget(slider, rowY);
	}

	private void addParticleOpacitySliderRow(Config config, int rowX, int rowY, int labelWidth, String label) {
		@NonNull String displayLabel = requireNonNullString(label);
		addScrollableWidget(new StringWidget(rowX, rowY, labelWidth, 20, Component.literal(displayLabel), this.font), rowY);
		EditBox valueBox = new EditBox(this.font, rowX + labelWidth + LABEL_TO_VALUE_GAP, rowY, VALUE_BOX_WIDTH, 20, Component.literal(displayLabel));
		IntegerSlider slider = new IntegerSlider(
			rowX + labelWidth + LABEL_TO_VALUE_GAP + VALUE_BOX_WIDTH + VALUE_TO_SLIDER_GAP,
			rowY,
			SLIDER_WIDTH,
			20,
			0,
			100,
			config.getParticleOpacity(),
			value -> {
				config.setParticleOpacity(value);
				valueBox.setValue(requireNonNullString(Integer.toString(value)));
			}
		);

		valueBox.setValue(requireNonNullString(Integer.toString(config.getParticleOpacity())));
		valueBox.setResponder(text -> {
			if (text == null || text.isBlank()) {
				return;
			}
			try {
				int parsed = Integer.parseInt(text);
				int clamped = Math.max(0, Math.min(100, parsed));
				config.setParticleOpacity(clamped);
				slider.setIntValue(clamped);
				if (clamped != parsed) {
					valueBox.setValue(requireNonNullString(Integer.toString(clamped)));
				}
			} catch (NumberFormatException ignored) {
			}
		});

		addScrollableWidget(valueBox, rowY);
		addScrollableWidget(slider, rowY);
	}

	private void addConfigurationHotkeyRow(Config config, int rowX, int rowY, int labelWidth, String label) {
		@NonNull String displayLabel = requireNonNullString(label);
		addScrollableWidget(new StringWidget(rowX, rowY, labelWidth, 20, Component.literal(displayLabel), this.font), rowY);
		int buttonX = rowX + labelWidth + LABEL_TO_VALUE_GAP;
		Button hotkeyButton = Button.builder(Component.empty(), button -> {
			listeningForConfigurationHotkey = true;
			refreshConfigurationHotkeyButtons(config);
		}).bounds(buttonX, rowY, HOTKEY_BUTTON_WIDTH, 20).build();
		Button resetButton = Button.builder(Component.literal("Reset"), button -> {
			config.resetConfigurationHotkeyDefaults();
			listeningForConfigurationHotkey = false;
			refreshConfigurationHotkeyButtons(config);
		}).bounds(buttonX + HOTKEY_BUTTON_WIDTH + HOTKEY_BUTTON_GAP, rowY, HOTKEY_RESET_BUTTON_WIDTH, 20).build();
		configurationHotkeyButton = addScrollableWidget(hotkeyButton, rowY);
		configurationHotkeyResetButton = addScrollableWidget(resetButton, rowY);
		refreshConfigurationHotkeyButtons(config);
	}

	private void refreshConfigurationHotkeyButtons(Config config) {
		Button hotkeyButton = configurationHotkeyButton;
		configurationHotkeyConflicts.clear();
		configurationHotkeyConflicts.addAll(getConfigurationHotkeyConflicts(config));
		configurationHotkeyHasConflict = !configurationHotkeyConflicts.isEmpty();
		if (hotkeyButton != null) {
			InputConstants.Key key = InputConstants.Type.KEYSYM.getOrCreate(config.getConfigurationHotkey());
			if (listeningForConfigurationHotkey) {
				hotkeyButton.setMessage(requireNonNullComponent(
					Component.empty()
						.append(Component.literal("> ").withStyle(ChatFormatting.YELLOW))
						.append(key.getDisplayName().copy().withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE))
						.append(Component.literal(" <").withStyle(ChatFormatting.YELLOW))
				));
				hotkeyButton.setTooltip(null);
			} else {
				if (configurationHotkeyHasConflict) {
					hotkeyButton.setMessage(requireNonNullComponent(
						Component.empty()
							.append(Component.literal("[ ").withStyle(ChatFormatting.YELLOW))
							.append(key.getDisplayName().copy())
							.append(Component.literal(" ]").withStyle(ChatFormatting.YELLOW))
					));
					MutableComponent tooltipText = Component.literal("This key is also used for:");
					for (Component conflict : configurationHotkeyConflicts) {
						tooltipText.append(Component.literal("\n"));
						tooltipText.append(Component.literal(requireNonNullString(requireNonNullComponent(conflict).getString())));
					}
					hotkeyButton.setTooltip(Tooltip.create(tooltipText));
				} else {
					hotkeyButton.setMessage(requireNonNullComponent(key.getDisplayName()));
					hotkeyButton.setTooltip(null);
				}
			}
		}
		Button resetButton = configurationHotkeyResetButton;
		if (resetButton != null) {
			resetButton.active = config.getConfigurationHotkey() != config.getDefaultConfigurationHotkey();
		}
	}

	private void addSingleOptionToggleButton(String label, String configKey, int x, int y, int width) {
		Config config = Config.getInstance();
		Button button = Button.builder(Component.empty(), clickedButton -> {
			boolean newValue = !config.isEnabled(configKey);
			config.setEnabled(configKey, newValue);
			clickedButton.setMessage(Component.literal(requireNonNullString(buildSingleOptionToggleText(label, newValue))));
		}).bounds(x, y, width, 20).build();
		button.setMessage(Component.literal(requireNonNullString(buildSingleOptionToggleText(label, config.isEnabled(configKey)))));
		addScrollableWidget(button, y);
	}

	private String buildSingleOptionToggleText(String label, boolean enabled) {
		@NonNull String safeLabel = requireNonNullString(label);
		return safeLabel + ": " + (enabled ? "ON" : "OFF");
	}

	private <T extends net.minecraft.client.gui.components.AbstractWidget> T addScrollableWidget(T widget, int baseY) {
		this.addWidget(widget);
		scrollableWidgets.add(widget);
		scrollBaseY.put(widget, baseY);
		return widget;
	}

	private void updateScrollBounds() {
		int contentBottom = CONTENT_TOP_Y;
		for (net.minecraft.client.gui.components.AbstractWidget widget : scrollableWidgets) {
			Integer baseY = scrollBaseY.get(widget);
			if (baseY != null) {
				contentBottom = Math.max(contentBottom, baseY + widget.getHeight());
			}
		}
		contentBottom += CONTENT_BOTTOM_PADDING;
		int visibleBottom = this.height - FOOTER_HEIGHT;
		maxScroll = Math.max(0, contentBottom - visibleBottom);
		if (scrollOffset > maxScroll) {
			scrollOffset = maxScroll;
		}
	}

	private void scrollBy(int delta) {
		if (maxScroll <= 0) {
			return;
		}
		scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset + delta));
		applyScrollAndVisibility();
	}

	private int getVisibleBottom() {
		return getViewportBorderBottom() - VIEWPORT_INSET_Y;
	}

	private int getVisibleTop() {
		return getViewportBorderTop() + VIEWPORT_INSET_Y;
	}

	private int getViewportBorderTop() {
		return CONTENT_TOP_Y;
	}

	private int getViewportBorderBottom() {
		return this.height - FOOTER_HEIGHT;
	}

	private int getViewportHeight() {
		return Math.max(1, getVisibleBottom() - getVisibleTop());
	}

	private int getScrollbarX() {
		return this.width - SCROLLBAR_MARGIN_RIGHT - SCROLLBAR_WIDTH;
	}

	private int getScrollbarTrackTop() {
		return getViewportBorderTop() + VIEWPORT_INSET_Y;
	}

	private int getScrollbarTrackBottom() {
		return getViewportBorderBottom() - VIEWPORT_INSET_Y;
	}

	private int getScrollbarTrackHeight() {
		return Math.max(1, getScrollbarTrackBottom() - getScrollbarTrackTop());
	}

	private boolean isInScrollbarTrack(double mouseX, double mouseY) {
		int x = getScrollbarX();
		int top = getScrollbarTrackTop();
		int bottom = getScrollbarTrackBottom();
		return mouseX >= x && mouseX < x + SCROLLBAR_WIDTH && mouseY >= top && mouseY < bottom;
	}

	private void setScrollFromHandleY(int handleY) {
		if (maxScroll <= 0) {
			return;
		}
		int trackTop = getScrollbarTrackTop();
		int handleHeight = getScrollbarHandleHeight();
		int travel = Math.max(1, getScrollbarTrackHeight() - handleHeight);
		int clampedHandleY = Math.max(trackTop, Math.min(trackTop + travel, handleY));
		double ratio = (clampedHandleY - trackTop) / (double) travel;
		scrollOffset = Math.max(0, Math.min(maxScroll, (int) Math.round(ratio * maxScroll)));
		applyScrollAndVisibility();
	}

	private void renderScrollableWidgets(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int scissorTop = getVisibleTop();
		int scissorBottom = getVisibleBottom();
		if (scissorBottom <= scissorTop) {
			return;
		}
		graphics.enableScissor(0, scissorTop, this.width, scissorBottom);
		for (net.minecraft.client.gui.components.AbstractWidget widget : scrollableWidgets) {
			if (widget.visible) {
				widget.extractRenderState(graphics, mouseX, mouseY, partialTick);
			}
		}
		graphics.disableScissor();
	}

	private void renderScrollbar(GuiGraphicsExtractor graphics) {
		if (maxScroll <= 0) {
			return;
		}

		int x = getScrollbarX();
		int top = getScrollbarTrackTop();
		int bottom = getScrollbarTrackBottom();
		int handleY = getScrollbarHandleY();
		int handleHeight = getScrollbarHandleHeight();
		int handleBottom = Math.min(bottom, handleY + handleHeight);

		graphics.fill(x, top, x + SCROLLBAR_WIDTH, bottom, SCROLLBAR_TRACK_COLOR);
		graphics.fill(x, top, x + 1, bottom, SCROLLBAR_BORDER_COLOR);
		graphics.fill(x + SCROLLBAR_WIDTH - 1, top, x + SCROLLBAR_WIDTH, bottom, SCROLLBAR_BORDER_COLOR);

		int handleColor = draggingScrollbar ? SCROLLBAR_HANDLE_DRAGGING_COLOR : SCROLLBAR_HANDLE_COLOR;
		graphics.fill(x, handleY, x + SCROLLBAR_WIDTH, handleBottom, handleColor);
	}

	private int getScrollbarHandleHeight() {
		if (maxScroll <= 0) {
			return getScrollbarTrackHeight();
		}
		int viewportHeight = getViewportHeight();
		int contentHeight = viewportHeight + maxScroll;
		int scaled = (int) Math.round((viewportHeight * (double) viewportHeight) / Math.max(1.0D, contentHeight));
		return Math.max(SCROLLBAR_MIN_HANDLE_HEIGHT, Math.min(getScrollbarTrackHeight(), scaled));
	}

	private int getScrollbarHandleY() {
		if (maxScroll <= 0) {
			return getScrollbarTrackTop();
		}
		int handleHeight = getScrollbarHandleHeight();
		int travel = Math.max(1, getScrollbarTrackHeight() - handleHeight);
		double ratio = scrollOffset / (double) maxScroll;
		return getScrollbarTrackTop() + (int) Math.round(ratio * travel);
	}

	private List<Component> getConfigurationHotkeyConflicts(Config config) {
		InputConstants.Key configuredKey = InputConstants.Type.KEYSYM.getOrCreate(config.getConfigurationHotkey());
		List<Component> conflicts = new ArrayList<>();
		for (KeyMapping keyMapping : this.minecraft.options.keyMappings) {
			if (keyMapping.matches(configuredKey)) {
				conflicts.add(Component.translatable(keyMapping.getName()));
			}
		}
		return conflicts;
	}

	private void applyScrollAndVisibility() {
		int visibleTop = getVisibleTop();
		int visibleBottom = getVisibleBottom();
		Config config = Config.getInstance();
		for (net.minecraft.client.gui.components.AbstractWidget widget : scrollableWidgets) {
			Integer baseY = scrollBaseY.get(widget);
			if (baseY == null) {
				continue;
			}
			int y = baseY - scrollOffset;
			widget.setY(y);
			boolean intersectsViewport = y < visibleBottom && (y + widget.getHeight()) > visibleTop;
			widget.visible = intersectsViewport;
			if (widget == configurationHotkeyResetButton) {
				widget.active = intersectsViewport && config.getConfigurationHotkey() != config.getDefaultConfigurationHotkey();
			} else {
				widget.active = intersectsViewport;
			}
		}
	}

	private static @NonNull String requireNonNullString(@Nullable String value) {
		if (value == null) {
			return "";
		}
		return value;
	}

	private static @NonNull Component requireNonNullComponent(@Nullable Component value) {
		if (value == null) {
			return Component.empty();
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
}