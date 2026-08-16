package salty5844.collisioneffects.client.effect.environment;

import salty5844.collisioneffects.client.util.TextureSelection;

import salty5844.collisioneffects.client.util.ParticleVisuals;

import salty5844.collisioneffects.client.util.GlobalParticleCapacity;

import salty5844.collisioneffects.client.config.Config;


import org.joml.Matrix3x2fStack;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import org.jspecify.annotations.NonNull;

public final class LavaSplatters {

	private static final long SPLAT_LIFETIME_MS = 1000L;
	private static final int MAX_SPLATS = 100;
	private static final int MAX_SPAWN_ATTEMPTS = 50;
	private static final float BASE_EXPONENT = 2.0F;
	private static final float SPLAT_OVERLAP_PADDING = 50.0F;
	private static final float SURFACE_SPAWN_RATE = 8.0F;
	private static final int BURST_COUNT_PER_TEXTURE = 2;

	private record LavaType(@NonNull Identifier texture, int textureSize, float exponentMultiplier) {}

	private static final LavaType[] SPLAT_TYPES = new LavaType[]{
		new LavaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/magma/g-small-1.png"), 16, 1.0F),
		new LavaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/magma/g-small-2.png"), 16, 1.0F),
		new LavaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/magma/g-small-3.png"), 16, 1.0F),
		new LavaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/magma/g-small-4.png"), 16, 1.0F),
		new LavaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/magma/g-medium-1.png"), 16, 1.0F),
		new LavaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/magma/g-medium-2.png"), 16, 1.0F),
		new LavaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/magma/g-medium-3.png"), 16, 1.0F),
		new LavaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/magma/g-medium-4.png"), 16, 1.0F),
		new LavaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/magma/g-large-1.png"), 16, 1.0F),
		new LavaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/magma/g-large-2.png"), 16, 1.0F),
		new LavaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/magma/g-large-3.png"), 16, 1.0F),
		new LavaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/magma/g-large-4.png"), 16, 1.0F)
	};

	private static final class LavaSplat {
		private float x;
		private float y;
		private float size;
		private float rotation;
		private boolean flipX;
		private boolean flipY;
		private Identifier texture;
		private int textureSize;
		private long spawnTime;
	}

	private final List<LavaSplat> splats = new ArrayList<>();
	private final Random random = new Random();
	private boolean wasInLava = false;
	private float surfaceSpawnAccumulator = 0.0F;
	private Identifier lastSpawnTexture;

	public void tickAndRender(
		GuiGraphicsExtractor graphics,
		long now,
		float elapsedSeconds,
		int width,
		int height,
		boolean enabled,
		boolean allowCurrentView,
		boolean freezeForPause,
		boolean inLava,
		boolean submerged,
		boolean lookingDown,
		boolean swimmingUp
	) {
		if (!enabled || !allowCurrentView) {
			this.clear();
			this.wasInLava = inLava;
			return;
		}

		boolean shouldSpawnForLookDirection = !freezeForPause && lookingDown;
		boolean enteredLava = shouldSpawnForLookDirection && inLava && !this.wasInLava;
		boolean exitedLava = shouldSpawnForLookDirection && !inLava && this.wasInLava;

		if (submerged) {
			this.clear();
			this.wasInLava = true;
			return;
		}

		if (enteredLava) {
			spawnBurst(width, height, BURST_COUNT_PER_TEXTURE, now);
		}

		if (shouldSpawnForLookDirection && inLava && swimmingUp) {
			this.surfaceSpawnAccumulator += SURFACE_SPAWN_RATE * elapsedSeconds;
			while (this.surfaceSpawnAccumulator >= 1.0F) {
				spawnOne(width, height, now);
				this.surfaceSpawnAccumulator -= 1.0F;
			}
		} else if (!freezeForPause) {
			this.surfaceSpawnAccumulator = 0.0F;
		}

		if (exitedLava) {
			spawnBurst(width, height, BURST_COUNT_PER_TEXTURE, now);
		}
		this.wasInLava = inLava;

		int removedSplats = this.splats.size();
		this.splats.removeIf(splat -> now - splat.spawnTime > SPLAT_LIFETIME_MS);
		GlobalParticleCapacity.release(removedSplats - this.splats.size());
		renderSplats(graphics, now);
	}

	private void renderSplats(GuiGraphicsExtractor graphics, long now) {
		for (LavaSplat splat : this.splats) {
			float age = (now - splat.spawnTime) / (float) SPLAT_LIFETIME_MS;
			float alpha = 1.0F - age;
			if (alpha <= 0.0F) {
				continue;
			}

			int argb = ParticleVisuals.textureArgb(alpha);

			Matrix3x2fStack matrices = graphics.pose();
			matrices.pushMatrix();
			matrices.translate(splat.x, splat.y);

			float half = splat.size / 2.0F;
			float textureHalf = splat.textureSize / 2.0F;
			float drawScale = splat.size / splat.textureSize;
			matrices.translate(half, half);

			matrices.scale(splat.flipX ? -1.0F : 1.0F, splat.flipY ? -1.0F : 1.0F);
			matrices.rotate((float) Math.toRadians(splat.rotation));
			matrices.scale(drawScale, drawScale);
			matrices.translate(-textureHalf, -textureHalf);

			graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				Objects.requireNonNull(splat.texture),
				0, 0,
				0, 0,
				splat.textureSize, splat.textureSize,
				splat.textureSize, splat.textureSize,
				argb
			);

			matrices.popMatrix();
		}
	}

	private void spawnBurst(int width, int height, int repeatsPerTexture, long now) {
		List<LavaType> burstTypes = new ArrayList<>();
		for (LavaType splatType : SPLAT_TYPES) {
			int weightedRepeats = repeatsPerTexture * ParticleVisuals.filenameSizeWeight(splatType.texture());
			for (int i = 0; i < weightedRepeats; i++) {
				burstTypes.add(splatType);
			}
		}
		while (!burstTypes.isEmpty()) {
			LavaType splatType = TextureSelection.popRandomAvoidingRepeat(burstTypes, random, this.lastSpawnTexture, entry -> entry.texture());
			if (splatType == null) {
				break;
			}
			spawnBurstSplat(width, height, now, splatType);
		}
	}

	private void spawnOne(int width, int height, long now) {
		LavaType splatType = pickSplatType();
		if (splatType == null) {
			return;
		}
		spawnOne(width, height, now, splatType);
	}

	private void spawnOne(int width, int height, long now, LavaType splatType) {
		spawnBurstSplat(width, height, now, splatType);
	}

	private void spawnBurstSplat(int width, int height, long now, LavaType splatType) {

		float centerX = width / 2.0F;
		float centerY = height / 2.0F;
		float halfWidth = width * 0.5F;
		float halfHeight = height * 0.5F;
		float maxRadius = (float) Math.sqrt((halfWidth * halfWidth) + (halfHeight * halfHeight));
		float deadZone = maxRadius * (Config.getInstance().getCenterDeadzone() / 100.0F);
		if (deadZone >= maxRadius - 0.0001F) {
			return;
		}
		float size = splatType.textureSize() * 2.5F;
		float exponent = BASE_EXPONENT;

		float x = 0.0F;
		float y = 0.0F;
		boolean foundPosition = false;
		for (int attempt = 0; attempt < MAX_SPAWN_ATTEMPTS; attempt++) {
			x = random.nextFloat() * width;
			y = (height * 0.5F) + (random.nextFloat() * height * 0.5F);

			float candidateCenterX = x + (size / 2.0F);
			float candidateCenterY = y + (size / 2.0F);
			float dx = candidateCenterX - centerX;
			float dy = candidateCenterY - centerY;
			float distance = (float) Math.sqrt(dx * dx + dy * dy);
			if (distance < deadZone) {
				continue;
			}

			float normalized = (distance - deadZone) / (maxRadius - deadZone);
			float chance = (float) Math.pow(normalized, exponent);
			if (random.nextFloat() > chance) {
				continue;
			}

			if (overlapsExistingSplat(x, y, size)) {
				continue;
			}

			foundPosition = true;
			break;
		}

		if (!foundPosition || this.splats.size() >= MAX_SPLATS) {
			return;
		}

		if (!GlobalParticleCapacity.tryAcquire()) {
			return;
		}

		LavaSplat splat = new LavaSplat();
		splat.size = size;
		splat.rotation = random.nextFloat() * 20.0F - 10.0F;
		splat.flipX = random.nextBoolean();
		splat.flipY = random.nextBoolean();
		splat.x = x;
		splat.y = y;
		splat.texture = splatType.texture();
		splat.textureSize = splatType.textureSize();
		splat.spawnTime = now;
		this.splats.add(splat);
		this.lastSpawnTexture = splat.texture;
	}

	private LavaType pickSplatType() {
		if (SPLAT_TYPES.length == 0) {
			return null;
		}
		List<@NonNull LavaType> weightedTypes = new ArrayList<>();
		for (LavaType splatType : SPLAT_TYPES) {
			for (int i = 0; i < ParticleVisuals.filenameSizeWeight(splatType.texture()); i++) {
				weightedTypes.add(splatType);
			}
		}
		LavaType candidate = TextureSelection.popRandomAvoidingRepeat(weightedTypes, random, this.lastSpawnTexture, entry -> entry.texture());
		if (candidate != null) {
			return candidate;
		}
		return SPLAT_TYPES[random.nextInt(SPLAT_TYPES.length)];
	}

	private boolean overlapsExistingSplat(float x, float y, float size) {
		float radius = size / 2.0F;
		float centerX = x + radius;
		float centerY = y + radius;

		for (LavaSplat existing : this.splats) {
			float existingRadius = existing.size / 2.0F;
			float existingCenterX = existing.x + existingRadius;
			float existingCenterY = existing.y + existingRadius;

			float dx = centerX - existingCenterX;
			float dy = centerY - existingCenterY;
			float minimumDistance = radius + existingRadius + SPLAT_OVERLAP_PADDING;
			if (dx * dx + dy * dy < minimumDistance * minimumDistance) {
				return true;
			}
		}

		return false;
	}

	public void clear() {
		GlobalParticleCapacity.release(this.splats.size());
		this.splats.clear();
		this.surfaceSpawnAccumulator = 0.0F;
		this.lastSpawnTexture = null;
	}
}
