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

public final class WaterDrops {

	private static final long DROP_LIFETIME_MS = 1000L;
	private static final int MAX_DROPS = 100;
	private static final int MAX_SPAWN_ATTEMPTS = 50;
	private static final float BASE_EXPONENT = 2.0F;
	private static final float SPLAT_OVERLAP_PADDING = 50.0F;
	private static final float SURFACE_SPAWN_RATE = 8.0F;
	private static final int BURST_COUNT_PER_TEXTURE = 3;

	private record WaterDropType(@NonNull Identifier texture, int textureSize, float exponentMultiplier) {}

	private static final WaterDropType[] DROP_TYPES = new WaterDropType[]{
		new WaterDropType(Identifier.fromNamespaceAndPath("collision-effects", "textures/splashes/small.png"), 16, 1.0F),
		new WaterDropType(Identifier.fromNamespaceAndPath("collision-effects", "textures/splashes/medium.png"), 16, 1.0F),
		new WaterDropType(Identifier.fromNamespaceAndPath("collision-effects", "textures/splashes/large.png"), 16, 1.0F)
	};

	private static final List<@NonNull WaterDropType> WEIGHTED_BURST_TYPES =
		ParticleVisuals.buildWeightedPool(DROP_TYPES, BURST_COUNT_PER_TEXTURE, WaterDropType::texture);
	private static final List<@NonNull WaterDropType> WEIGHTED_DROP_TYPES =
		ParticleVisuals.buildWeightedPool(DROP_TYPES, 1, WaterDropType::texture);

	private static final class WaterDrop {
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

	private final List<WaterDrop> drops = new ArrayList<>();
	private final Random random = new Random();
	private boolean wasInWater = false;
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
		boolean inWater,
		boolean submerged,
		boolean inRain,
		boolean lookingDown,
		boolean swimmingUp
	) {
		if (!enabled || !allowCurrentView) {
			this.clear();
			this.wasInWater = inWater;
			return;
		}

		boolean shouldSpawnForLookDirection = !freezeForPause && lookingDown;
		boolean enteredWater = shouldSpawnForLookDirection && inWater && !this.wasInWater;
		boolean exitedWater = shouldSpawnForLookDirection && !inWater && this.wasInWater;

		if (submerged) {
			this.clear();
			this.wasInWater = true;
			return;
		}

		if (enteredWater) {
			spawnBurst(width, height, now);
		}

		if (shouldSpawnForLookDirection && inWater && swimmingUp) {
			this.surfaceSpawnAccumulator += SURFACE_SPAWN_RATE * elapsedSeconds;
			while (this.surfaceSpawnAccumulator >= 1.0F) {
				spawnOne(width, height, now);
				this.surfaceSpawnAccumulator -= 1.0F;
			}
		} else if (!freezeForPause) {
			this.surfaceSpawnAccumulator = 0.0F;
		}

		if (exitedWater) {
			spawnBurst(width, height, now);
		}
		this.wasInWater = inWater;

		int removedDrops = this.drops.size();
		this.drops.removeIf(drop -> now - drop.spawnTime > DROP_LIFETIME_MS);
		GlobalParticleCapacity.release(removedDrops - this.drops.size());
		renderDrops(graphics, now);
	}

	private void renderDrops(GuiGraphicsExtractor graphics, long now) {
		for (WaterDrop drop : this.drops) {
			float age = (now - drop.spawnTime) / (float) DROP_LIFETIME_MS;
			float alpha = 1.0F - age;
			if (alpha <= 0.0F) {
				continue;
			}

			int argb = ParticleVisuals.textureArgb(alpha);

			Matrix3x2fStack matrices = graphics.pose();
			matrices.pushMatrix();
			matrices.translate(drop.x, drop.y);

			float half = drop.size / 2.0F;
			float textureHalf = drop.textureSize / 2.0F;
			float drawScale = drop.size / drop.textureSize;
			matrices.translate(half, half);

			matrices.scale(drop.flipX ? -1.0F : 1.0F, drop.flipY ? -1.0F : 1.0F);
			matrices.rotate((float) Math.toRadians(drop.rotation));
			matrices.scale(drawScale, drawScale);
			matrices.translate(-textureHalf, -textureHalf);

			graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				Objects.requireNonNull(drop.texture),
				0, 0,
				0, 0,
				drop.textureSize, drop.textureSize,
				drop.textureSize, drop.textureSize,
				argb
			);

			matrices.popMatrix();
		}
	}

	private void spawnBurst(int width, int height, long now) {
		List<WaterDropType> burstTypes = new ArrayList<>(WEIGHTED_BURST_TYPES);
		while (!burstTypes.isEmpty()) {
			WaterDropType dropType = TextureSelection.popRandomAvoidingRepeat(burstTypes, random, this.lastSpawnTexture, entry -> entry.texture());
			if (dropType == null) {
				break;
			}
			spawnBurstDrop(width, height, now, dropType);
		}
	}

	private void spawnOne(int width, int height, long now) {
		WaterDropType dropType = pickDropType();
		if (dropType == null) {
			return;
		}
		spawnOne(width, height, now, dropType);
	}

	private void spawnOne(int width, int height, long now, WaterDropType dropType) {
		spawnBurstDrop(width, height, now, dropType);
	}

	private void spawnBurstDrop(int width, int height, long now, WaterDropType dropType) {

		float centerX = width / 2.0F;
		float centerY = height / 2.0F;
		float halfWidth = width * 0.5F;
		float halfHeight = height * 0.5F;
		float maxRadius = (float) Math.sqrt((halfWidth * halfWidth) + (halfHeight * halfHeight));
		float deadZone = maxRadius * (Config.getInstance().getCenterDeadzone() / 100.0F);
		if (deadZone >= maxRadius - 0.0001F) {
			return;
		}
		float size = dropType.textureSize() * 2.5F;
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

			if (overlapsExistingDrop(x, y, size)) {
				continue;
			}

			foundPosition = true;
			break;
		}

		if (!foundPosition || this.drops.size() >= MAX_DROPS) {
			return;
		}

		if (!GlobalParticleCapacity.tryAcquire()) {
			return;
		}

		WaterDrop drop = new WaterDrop();
		drop.size = size;
		drop.rotation = random.nextFloat() * 20.0F - 10.0F;
		drop.flipX = random.nextBoolean();
		drop.flipY = random.nextBoolean();
		drop.x = x;
		drop.y = y;
		drop.texture = dropType.texture();
		drop.textureSize = dropType.textureSize();
		drop.spawnTime = now;
		this.drops.add(drop);
		this.lastSpawnTexture = drop.texture;
	}

	private WaterDropType pickDropType() {
		if (DROP_TYPES.length == 0) {
			return null;
		}
		List<@NonNull WaterDropType> weightedTypes = new ArrayList<>(WEIGHTED_DROP_TYPES);
		WaterDropType candidate = TextureSelection.popRandomAvoidingRepeat(weightedTypes, random, this.lastSpawnTexture, entry -> entry.texture());
		if (candidate != null) {
			return candidate;
		}
		return DROP_TYPES[random.nextInt(DROP_TYPES.length)];
	}

	private boolean overlapsExistingDrop(float x, float y, float size) {
		float radius = size / 2.0F;
		float centerX = x + radius;
		float centerY = y + radius;

		for (WaterDrop existing : this.drops) {
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
		GlobalParticleCapacity.release(this.drops.size());
		this.drops.clear();
		this.surfaceSpawnAccumulator = 0.0F;
		this.lastSpawnTexture = null;
	}
}