package salty5844.collisioneffects.client.effect.overlay;

import salty5844.collisioneffects.client.util.ParticleVisuals;

import java.util.Objects;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.resources.ResourceLocation;

public final class SquidInkVignette {

	private static final ResourceLocation VIGNETTE_TEXTURE = Objects.requireNonNull(new ResourceLocation("collision-effects", "textures/vignette.png"));
	private static final int VIGNETTE_TEXTURE_WIDTH = 114;
	private static final int VIGNETTE_TEXTURE_HEIGHT = 64;
	private static final float MAX_ALPHA = 0.80F;
	private static final float FADE_IN_PER_SECOND = 5.5F;
	private static final float FADE_OUT_PER_SECOND = 3.0F;

	private long lastUpdateMillis = -1L;
	private float currentAlpha;

	public void tickAndRender(
		GuiGraphics graphics,
		long now,
		int width,
		int height,
		boolean enabled,
		boolean allowCurrentView,
		boolean freezeForPause,
		boolean inInk
	) {
		if (!enabled || !allowCurrentView) {
			clear();
			return;
		}

		if (!freezeForPause) {
			if (lastUpdateMillis < 0L) {
				lastUpdateMillis = now;
			}
			float deltaSeconds = Math.max(0.0F, Math.min(0.25F, (now - lastUpdateMillis) / 1000.0F));
			lastUpdateMillis = now;

			float targetAlpha = inInk ? MAX_ALPHA : 0.0F;
			float speed = inInk ? FADE_IN_PER_SECOND : FADE_OUT_PER_SECOND;
			float step = speed * deltaSeconds;
			if (currentAlpha < targetAlpha) {
				currentAlpha = Math.min(targetAlpha, currentAlpha + step);
			} else if (currentAlpha > targetAlpha) {
				currentAlpha = Math.max(targetAlpha, currentAlpha - step);
			}
		}

		renderVignette(graphics, width, height, currentAlpha);
	}

	public void clear() {
		lastUpdateMillis = -1L;
		currentAlpha = 0.0F;
	}

	private void renderVignette(GuiGraphics graphics, int width, int height, float alpha) {
		if (alpha <= 0.0F || width <= 0 || height <= 0) {
			return;
		}

		PoseStack matrices = graphics.pose();
		matrices.pushPose();
		matrices.scale(width / (float) VIGNETTE_TEXTURE_WIDTH, height / (float) VIGNETTE_TEXTURE_HEIGHT, 1.0F);
		ParticleVisuals.blitTinted(graphics, VIGNETTE_TEXTURE, VIGNETTE_TEXTURE_WIDTH, VIGNETTE_TEXTURE_HEIGHT, Math.max(0.0F, Math.min(1.0F, alpha)));
		matrices.popPose();
	}
}