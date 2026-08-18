package salty5844.collisioneffects.client.effect.environment;

import salty5844.collisioneffects.client.util.ParticleVisuals;

import salty5844.collisioneffects.client.util.GlobalParticleCapacity;

import salty5844.collisioneffects.client.config.Config;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public final class Snowflakes {

	private static final long FLAKE_LIFETIME_MS = 2000L;
	private static final int MAX_FLAKES = 100;
	private static final int MAX_SPAWN_ATTEMPTS = 50;
	private static final float BASE_EXPONENT = 2.0F;
	private static final float SPLAT_OVERLAP_PADDING = 50.0F;
	private static final float FLYING_SPAWN_RATE_MULTIPLIER = 1.4F;
	private static final float FLYING_START_PITCH = 30.0F;
	private static final float DESCENT_LOOK_FULL_PITCH = 85.0F;
	private static final double DESCENT_START_SPEED = 0.35D;
	private static final double DESCENT_FULL_SPEED = 1.1D;
	private static final float GLOBAL_SNOW_RATE_MULTIPLIER = 0.65F;

	private record SnowflakeType(ResourceLocation texture, int textureSize, float exponentMultiplier) {}

	private static final SnowflakeType[] FLAKE_TYPES = new SnowflakeType[]{
		new SnowflakeType(new ResourceLocation("collision-effects", "textures/flakes/small.png"), 16, 1.0F),
		new SnowflakeType(new ResourceLocation("collision-effects", "textures/flakes/medium.png"), 16, 1.25F),
		new SnowflakeType(new ResourceLocation("collision-effects", "textures/flakes/large.png"), 16, 1.5F)
	};

	private static final class Snowflake {
		private float x;
		private float y;
		private float size;
		private float rotation;
		private boolean flipX;
		private boolean flipY;
		private ResourceLocation texture;
		private int textureSize;
		private long spawnTime;
	}

	private final List<Snowflake> flakes = new ArrayList<>();
	private final Random random = new Random();
	private float spawnAccumulator = 0.0F;

	public void tickAndRender(
		GuiGraphics graphics,
		long now,
		float elapsedSeconds,
		int width,
		int height,
		boolean enabled,
		boolean allowCurrentView,
		boolean freezeForPause,
		boolean inSnow,
		float baseSpawnRate,
		boolean flying,
		float pitch,
		double speed,
		double verticalSpeed
	) {
		if (!enabled || !allowCurrentView) {
			this.clear();
			return;
		}

		float spawnRate = getSpawnRate(baseSpawnRate, pitch, speed, verticalSpeed);
		if (!freezeForPause && !inSnow) {
			// Stop spawning immediately but let existing flakes expire naturally.
			this.spawnAccumulator = 0.0F;
		}

		if (!freezeForPause && inSnow && spawnRate > 0.0F) {
			this.spawnAccumulator += spawnRate * elapsedSeconds;
			while (this.spawnAccumulator >= 1.0F) {
				spawnOne(width, height, now);
				this.spawnAccumulator -= 1.0F;
			}
		} else if (!freezeForPause) {
			this.spawnAccumulator = 0.0F;
		}

		int removedFlakes = this.flakes.size();
		this.flakes.removeIf(flake -> now - flake.spawnTime > FLAKE_LIFETIME_MS);
		GlobalParticleCapacity.release(removedFlakes - this.flakes.size());
		renderFlakes(graphics, now);
	}

	private float getSpawnRate(float baseSpawnRate, float pitch, double speed, double verticalSpeed) {
		if (pitch > FLYING_START_PITCH) {
			double downwardSpeed = Math.max(0.0D, -verticalSpeed);
			float descentFactor = (float) Math.max(0.0D, Math.min(1.0D,
				(downwardSpeed - DESCENT_START_SPEED) / (DESCENT_FULL_SPEED - DESCENT_START_SPEED)));
			if (descentFactor <= 0.0F) {
				return 0.0F;
			}

			float lookDownFactor = Math.max(0.0F, Math.min(1.0F,
				(pitch - FLYING_START_PITCH) / (DESCENT_LOOK_FULL_PITCH - FLYING_START_PITCH)));
			float lookDownSuppression = 1.0F - (0.65F * lookDownFactor);
			return baseSpawnRate
				* FLYING_SPAWN_RATE_MULTIPLIER
				* GLOBAL_SNOW_RATE_MULTIPLIER
				* descentFactor
				* lookDownSuppression;
		}

		float lookFactor = Math.max(0.0F, Math.min(1.0F, (FLYING_START_PITCH - pitch) / (FLYING_START_PITCH + 90.0F)));
		float speedFactor = Math.max(0.0F, Math.min(1.0F, (float) speed));
		float lookDownFactor = Math.max(0.0F, Math.min(1.0F, pitch / FLYING_START_PITCH));
		float lookDownSuppression = 1.0F - (0.45F * lookDownFactor * lookDownFactor);
		return baseSpawnRate
			* FLYING_SPAWN_RATE_MULTIPLIER
			* GLOBAL_SNOW_RATE_MULTIPLIER
			* (0.75F + 0.25F * lookFactor)
			* (0.6F + 0.4F * speedFactor)
			* lookDownSuppression;
	}

	private void spawnOne(int width, int height, long now) {
		if (this.flakes.size() >= MAX_FLAKES) {
			return;
		}

		SnowflakeType type = pickSnowflakeType();
		if (type == null) {
			return;
		}
		float centerX = width / 2.0F;
		float centerY = height / 2.0F;
		float halfWidth = width * 0.5F;
		float halfHeight = height * 0.5F;
		float maxRadius = (float) Math.sqrt((halfWidth * halfWidth) + (halfHeight * halfHeight));
		float deadZone = maxRadius * (Config.getInstance().getCenterDeadzone() / 100.0F);
		if (deadZone >= maxRadius - 0.0001F) {
			return;
		}
		float size = type.textureSize() * 2.5F;
		float exponent = BASE_EXPONENT;

		float x = 0.0F;
		float y = 0.0F;
		boolean foundPosition = false;
		for (int attempt = 0; attempt < MAX_SPAWN_ATTEMPTS; attempt++) {
			x = random.nextFloat() * width;
			y = random.nextFloat() * height;

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

			if (overlapsExistingFlake(x, y, size)) {
				continue;
			}

			foundPosition = true;
			break;
		}

		if (!foundPosition) {
			return;
		}

		if (!GlobalParticleCapacity.tryAcquire()) {
			return;
		}

		Snowflake flake = new Snowflake();
		flake.size = size;
		flake.rotation = random.nextFloat() * 20.0F - 10.0F;
		flake.flipX = random.nextBoolean();
		flake.flipY = random.nextBoolean();
		flake.x = x;
		flake.y = y;
		flake.texture = type.texture();
		flake.textureSize = type.textureSize();
		flake.spawnTime = now;
		this.flakes.add(flake);
	}

	private boolean overlapsExistingFlake(float x, float y, float size) {
		float radius = size / 2.0F;
		float centerX = x + radius;
		float centerY = y + radius;

		for (Snowflake existing : this.flakes) {
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

	private SnowflakeType pickSnowflakeType() {
		if (FLAKE_TYPES.length == 0) {
			return null;
		}
		List<SnowflakeType> weightedTypes = new ArrayList<>();
		for (SnowflakeType type : FLAKE_TYPES) {
			for (int i = 0; i < ParticleVisuals.filenameSizeWeight(type.texture()); i++) {
				weightedTypes.add(type);
			}
		}
		return weightedTypes.get(random.nextInt(weightedTypes.size()));
	}

	private void renderFlakes(GuiGraphics graphics, long now) {
		for (Snowflake flake : this.flakes) {
			float age = (now - flake.spawnTime) / (float) FLAKE_LIFETIME_MS;
			float alpha = 1.0F - age;
			if (alpha <= 0.0F) {
				continue;
			}

			int argb = ParticleVisuals.textureArgb(alpha);

			PoseStack matrices = graphics.pose();
			matrices.pushPose();
			matrices.translate(flake.x, flake.y, 0.0F);

			float half = flake.size / 2.0F;
			float textureHalf = flake.textureSize / 2.0F;
			float drawScale = flake.size / flake.textureSize;
			matrices.translate(half, half, 0.0F);

			matrices.scale(flake.flipX ? -1.0F : 1.0F, flake.flipY ? -1.0F : 1.0F, 1.0F);
			matrices.mulPose(Axis.ZP.rotation((float) Math.toRadians(flake.rotation)));
			matrices.scale(drawScale, drawScale, 1.0F);
			matrices.translate(-textureHalf, -textureHalf, 0.0F);

			graphics.blit(
				
				Objects.requireNonNull(flake.texture),
				0, 0,
				0, 0,
				flake.textureSize, flake.textureSize,
				flake.textureSize, flake.textureSize,
				argb
			);

			matrices.popPose();
		}
	}

	public void clear() {
		GlobalParticleCapacity.release(this.flakes.size());
		this.flakes.clear();
		this.spawnAccumulator = 0.0F;
	}
}