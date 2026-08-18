package salty5844.collisioneffects.client.effect.overlay;

import java.util.Objects;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class DamageVingette {

	private static final ResourceLocation VIGNETTE_TEXTURE = Objects.requireNonNull(new ResourceLocation("collision-effects", "textures/vignette.png"));
	private static final int VIGNETTE_TEXTURE_WIDTH = 114;
	private static final int VIGNETTE_TEXTURE_HEIGHT = 64;
	private static final float MAX_TINT_ALPHA = 0.25F;

	public void tickAndRender(
		GuiGraphics graphics,
		int width,
		int height,
		boolean vingetteEnabled,
		boolean tintEnabled,
		boolean allowCurrentView,
		float healthRatio
	) {
		if ((!vingetteEnabled && !tintEnabled) || !allowCurrentView) {
			return;
		}

		float clampedHealth = Math.max(0.0F, Math.min(1.0F, healthRatio));
		float alpha = 1.0F - clampedHealth;
		if (alpha <= 0.0F) {
			return;
		}

		if (tintEnabled) {
			renderTint(graphics, width, height, alpha * MAX_TINT_ALPHA);
		}
		if (vingetteEnabled) {
			renderVingette(graphics, width, height, alpha);
		}
	}

	public void clear() {
		// Stateless: nothing to clear.
	}

	private void renderTint(GuiGraphics graphics, int width, int height, float alpha) {
		if (width <= 0 || height <= 0) {
			return;
		}

		int argb = ((int) (alpha * 255.0F) << 24) | 0x00FF0000;
		graphics.fill(0, 0, width, height, argb);
	}

	private void renderVingette(GuiGraphics graphics, int width, int height, float alpha) {
		if (width <= 0 || height <= 0) {
			return;
		}

		int argb = ((int) (alpha * 255.0F) << 24) | 0x00FFFFFF;
		PoseStack matrices = graphics.pose();
		matrices.pushPose();
		matrices.scale(width / (float) VIGNETTE_TEXTURE_WIDTH, height / (float) VIGNETTE_TEXTURE_HEIGHT, 1.0F);
		graphics.blit(
			VIGNETTE_TEXTURE,
			0, 0,
			0, 0,
			VIGNETTE_TEXTURE_WIDTH, VIGNETTE_TEXTURE_HEIGHT,
			VIGNETTE_TEXTURE_WIDTH, VIGNETTE_TEXTURE_HEIGHT,
			argb
		);
		matrices.popPose();
	}
}
