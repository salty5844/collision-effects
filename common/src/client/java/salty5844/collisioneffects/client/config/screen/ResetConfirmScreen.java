package salty5844.collisioneffects.client.config.screen;

import salty5844.collisioneffects.client.config.Config;




import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ResetConfirmScreen extends Screen {

	private final Screen parent;

	public ResetConfirmScreen(Screen parent) {
		super(Component.literal("Confirm Reset"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		String titleText = "Confirm Reset";
		int titleWidth = this.font.width(titleText);
		this.addRenderableWidget(new StringWidget((this.width - titleWidth) / 2, 24, titleWidth, 20, Component.literal(titleText), this.font));

		String messageText = "Reset all Collision Effects configuration?";
		int messageWidth = this.font.width(messageText);
		this.addRenderableWidget(new StringWidget((this.width - messageWidth) / 2, 52, messageWidth, 20, Component.literal(messageText), this.font));

		this.addRenderableWidget(Button.builder(Component.literal("Yes, reset"), button -> {
			Config.getInstance().resetToDefaults();
			Config.getInstance().save(Config.getConfigDir());
			if (this.minecraft != null && parent != null) {
				this.minecraft.setScreen(parent);
			}
		}).bounds(centerX - 102, this.height - 48, 100, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> {
			this.onClose();
		}).bounds(centerX + 2, this.height - 48, 100, 20).build());
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
}
