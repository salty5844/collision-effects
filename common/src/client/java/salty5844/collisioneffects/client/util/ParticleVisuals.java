package salty5844.collisioneffects.client.util;

import salty5844.collisioneffects.client.config.Config;


import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class ParticleVisuals {

	private ParticleVisuals() {
	}

	public static int textureArgb(float alpha) {
		float clampedAlpha = Math.max(0.0F, Math.min(1.0F, alpha));
		float opacityScale = Config.getInstance().getParticleOpacity() / 100.0F;
		float finalAlpha = Math.max(0.0F, Math.min(1.0F, clampedAlpha * opacityScale));
		return ((int) (finalAlpha * 255.0F) << 24) | 0x00FFFFFF;
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

	public static int filenameSizeWeight(Identifier texture) {
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
	public static <T> List<T> buildWeightedPool(T[] types, int repeatsPerTexture, Function<T, Identifier> textureResolver) {
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
