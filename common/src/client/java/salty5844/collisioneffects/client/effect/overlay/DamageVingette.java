package salty5844.collisioneffects.client.effect.overlay;

import java.util.Objects;

import org.joml.Matrix3x2fStack;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public final class DamageVingette {

	private static final @NonNull Identifier VIGNETTE_TEXTURE = Objects.requireNonNull(Identifier.fromNamespaceAndPath("collision-effects", "textures/vignette.png"));
	private static final int VIGNETTE_TEXTURE_WIDTH = 114;
	private static final int VIGNETTE_TEXTURE_HEIGHT = 64;
	private static final float MAX_TINT_ALPHA = 0.25F;

	public void tickAndRender(
		GuiGraphicsExtractor graphics,
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

	private void renderTint(GuiGraphicsExtractor graphics, int width, int height, float alpha) {
		if (width <= 0 || height <= 0) {
			return;
		}

		int argb = ((int) (alpha * 255.0F) << 24) | 0x00FF0000;
		graphics.fill(0, 0, width, height, argb);
	}

	private void renderVingette(GuiGraphicsExtractor graphics, int width, int height, float alpha) {
		if (width <= 0 || height <= 0) {
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
