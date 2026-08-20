package salty5844.collisioneffects.client.util;

import salty5844.collisioneffects.client.config.Config;


import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class ParticleVisuals {

	private ParticleVisuals() {
	}

	public static float textureAlpha(float alpha) {
		float clampedAlpha = Math.max(0.0F, Math.min(1.0F, alpha));
		float opacityScale = Config.getInstance().getParticleOpacity() / 100.0F;
		return Math.max(0.0F, Math.min(1.0F, clampedAlpha * opacityScale));
	}

	// 1.20 GuiGraphics has no tinted blit overload, so the alpha has to be applied through setColor.
	public static void blitTinted(GuiGraphics graphics, ResourceLocation texture, int width, int height, float alpha) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
		graphics.blit(texture, 0, 0, 0.0F, 0.0F, width, height, width, height);
		graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableBlend();
	}

	public static void beginBatch() {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
	}

	public static void endBatch() {
		RenderSystem.disableBlend();
	}

	public static int sizeWeight(int textureSize) {
		return switch (textureSize) {
			case 16 -> 4;
			case 32 -> 3;
			case 48 -> 2;
			case 64 -> 1;
			default -> 1;
		};
	}

	public static int filenameSizeWeight(ResourceLocation texture) {
		String path = texture.getPath();
		if (path.contains("small")) {
			return 4;
		}
		if (path.contains("medium")) {
			return 3;
		}
		if (path.contains("large")) {
			return 2;
		}
		return 1;
	}

	// Built once per effect class; callers copy it instead of re-deriving weights from filenames on every burst.
	public static <T> List<T> buildWeightedPool(T[] types, int repeatsPerTexture, Function<T, ResourceLocation> textureResolver) {
		List<T> weighted = new ArrayList<>();
		for (T type : types) {
			int repeats = repeatsPerTexture * filenameSizeWeight(textureResolver.apply(type));
			for (int i = 0; i < repeats; i++) {
				weighted.add(type);
			}
		}
		return List.copyOf(weighted);
	}
}
