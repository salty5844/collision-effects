package salty5844.collisioneffects.client.effect.overlay;

import java.util.Objects;

import org.joml.Matrix3x2fStack;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public final class WitherVignette {

	private static final @NonNull Identifier VIGNETTE_TEXTURE = Objects.requireNonNull(Identifier.fromNamespaceAndPath("collision-effects", "textures/vignette.png"));
	private static final int VIGNETTE_TEXTURE_WIDTH = 114;
	private static final int VIGNETTE_TEXTURE_HEIGHT = 64;
	private static final float MAX_ALPHA = 0.80F;
	private static final float FADE_IN_PER_SECOND = 5.5F;
	private static final float FADE_OUT_PER_SECOND = 3.0F;

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
		boolean hasWither
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

			float targetAlpha = hasWither ? MAX_ALPHA : 0.0F;
			float speed = hasWither ? FADE_IN_PER_SECOND : FADE_OUT_PER_SECOND;
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

	private void renderVignette(GuiGraphicsExtractor graphics, int width, int height, float alpha) {
		if (alpha <= 0.0F || width <= 0 || height <= 0) {
			return;
		}

		int argb = ((int) (alpha * 255.0F) << 24) | 0x00FFFFFF;
		Matrix3x2fStack matrices = graphics.pose();
		matrices.pushMatrix();
		matrices.scale(width / (float) VIGNETTE_TEXTURE_WIDTH, height / (float) VIGNETTE_TEXTURE_HEIGHT);
		graphics.blit(
			RenderPipelines.GUI_TEXTURED,
			VIGNETTE_TEXTURE,
			0, 0,
			0, 0,
			VIGNETTE_TEXTURE_WIDTH, VIGNETTE_TEXTURE_HEIGHT,
			VIGNETTE_TEXTURE_WIDTH, VIGNETTE_TEXTURE_HEIGHT,
			argb
		);
		matrices.popMatrix();
	}
}
