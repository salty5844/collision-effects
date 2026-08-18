package salty5844.collisioneffects.client.util;


import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;

public final class TextureSelection {

	private TextureSelection() {
	}

	@SuppressWarnings("nullness")
	private static <T extends Object> T getNonNull(List<T> entries, int index) {
		return Objects.requireNonNull(entries.get(index));
	}

	@SuppressWarnings("nullness")
	private static <T extends Object> T removeNonNull(List<T> entries, int index) {
		return Objects.requireNonNull(entries.remove(index));
	}

	public static <T extends Object> T popRandomAvoidingRepeat(
			List<T> entries,
		Random random,
		ResourceLocation lastTexture,
			Function<T, ResourceLocation> textureResolver
	) {
		if (entries.isEmpty()) {
			return null;
		}

		int fallbackIndex = random.nextInt(entries.size());
		if (lastTexture == null || entries.size() == 1) {
			return removeNonNull(entries, fallbackIndex);
		}

		for (int attempt = 0; attempt < entries.size(); attempt++) {
			int index = random.nextInt(entries.size());
			T candidate = getNonNull(entries, index);
			if (!Objects.equals(textureResolver.apply(candidate), lastTexture)) {
				return removeNonNull(entries, index);
			}
		}

		return removeNonNull(entries, fallbackIndex);
	}
}
