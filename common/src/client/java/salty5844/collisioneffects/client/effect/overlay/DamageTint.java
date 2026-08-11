package salty5844.collisioneffects.client.effect.overlay;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class DamageTint {

	private static final long FADE_DURATION_MS = 250L;
	private static final float MAX_ALPHA = 0.55F;

	private long lastUpdateMillis = -1L;
	private float currentAlpha;

	public void tickAndRender(
		GuiGraphicsExtractor graphics,
		long now,
		int width,
		int height,
		boolean enabled,
		boolean allowCurrentView,
		boolean freezeForPause,
		boolean damageTriggered
	) {
		if (!enabled || !allowCurrentView) {
			clear();
			return;
		}

		if (damageTriggered) {
			currentAlpha = MAX_ALPHA;
		}

		if (!freezeForPause) {
			if (lastUpdateMillis < 0L) {
				lastUpdateMillis = now;
			}
			float deltaSeconds = Math.max(0.0F, Math.min(0.25F, (now - lastUpdateMillis) / 1000.0F));
			lastUpdateMillis = now;

			if (currentAlpha > 0.0F) {
				float fadeSpeedPerSecond = MAX_ALPHA / (FADE_DURATION_MS / 1000.0F);
				currentAlpha = Math.max(0.0F, currentAlpha - (fadeSpeedPerSecond * deltaSeconds));
			}
		}

		renderTint(graphics, width, height, currentAlpha);
	}

	public void clear() {
		lastUpdateMillis = -1L;
		currentAlpha = 0.0F;
	}

	private void renderTint(GuiGraphicsExtractor graphics, int width, int height, float alpha) {
		if (alpha <= 0.0F || width <= 0 || height <= 0) {
			return;
		}

		int argb = ((int) (alpha * 255.0F) << 24) | 0x00FF0000;
		graphics.fill(0, 0, width, height, argb);
	}
}
