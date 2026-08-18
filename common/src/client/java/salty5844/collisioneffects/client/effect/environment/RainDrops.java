package salty5844.collisioneffects.client.effect.environment;

import salty5844.collisioneffects.client.util.TextureSelection;

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

public final class RainDrops {

	private static final long SPLASH_LIFETIME_MS = 1000L;
	private static final long STREAM_LIFETIME_MS = 2000L;
	private static final int MAX_DROPS = 100;
	private static final int MAX_SPAWN_ATTEMPTS = 50;
	private static final float BASE_EXPONENT = 2.0F;
	private static final float STREAM_OVERLAP_PADDING = 25.0F;
	private static final float RAIN_DROP_OVERLAP_PADDING = 25.0F;
	private static final float STREAM_SPEED_PX_PER_SEC = 180.0F;
	private static final float FLYING_STREAM_SPEED_MULTIPLIER_PER_SPEED = 0.25F;
	private static final float MAX_FLYING_STREAM_SPEED_MULTIPLIER = 2.25F;
	private static final float FLYING_SPAWN_RATE_MULTIPLIER = 1.4F;
	private static final float FLYING_STREAM_MIN_CHANCE = 0.45F;
	private static final double FLYING_STREAM_FULL_SPEED = 1.0D;
	private static final double DESCENT_STREAM_START_SPEED = 0.35D;
	private static final double DESCENT_STREAM_FULL_SPEED = 1.1D;
	private static final float DESCENT_LOOK_FULL_PITCH = 85.0F;
	private static final float RADIAL_UPWARD_START_PITCH = 75.0F;
	private static final float FLYING_START_PITCH = 30.0F;
	private static final float STOP_DECEL_RATE = 4.0F; // velocity multiplier loss per second when freezing
	private static final double UPWARD_CONVERT_THRESHOLD = 0.05D;
	private static final double CONVERT_MAX_SPEED = 0.4D;
	private static final float MAX_CONVERT_RATE = 3.0F; // stream drops converted to splashes per second at peak

	private record RainDropType(ResourceLocation texture, int textureSize, float exponentMultiplier) {}

	private static final RainDropType[] SPLASH_TYPES = new RainDropType[]{
		new RainDropType(new ResourceLocation("collision-effects", "textures/splashes/small.png"), 16, 1.0F),
		new RainDropType(new ResourceLocation("collision-effects", "textures/splashes/medium.png"), 16, 1.25F),
		new RainDropType(new ResourceLocation("collision-effects", "textures/splashes/large.png"), 16, 1.5F)
	};

	private static final RainDropType[] STREAM_TYPES = new RainDropType[]{
		new RainDropType(new ResourceLocation("collision-effects", "textures/drops/1.png"), 16, 1.0F),
		new RainDropType(new ResourceLocation("collision-effects", "textures/drops/2.png"), 16, 1.0F)
	};

	private static final class RainDrop {
		private float x;
		private float y;
		private float velocityX;
		private float velocityY;
		private float size;
		private float rotation;
		private boolean flipX;
		private boolean flipY;
		private ResourceLocation texture;
		private int textureSize;
		private long lifetimeMs;
		private long spawnTime;
		private boolean stream;
		private float radialDirX; // normalized outward direction from center at spawn
		private float radialDirY;
	}

	private final List<RainDrop> drops = new ArrayList<>();
	private final Random random = new Random();
	private float spawnAccumulator = 0.0F;
	private float convertAccumulator = 0.0F;
	private ResourceLocation lastSpawnTexture;

	public void tickAndRender(
		GuiGraphics graphics,
		long now,
		float elapsedSeconds,
		int width,
		int height,
		boolean enabled,
		boolean allowCurrentView,
		boolean freezeForPause,
		boolean canSpawnRain,
		boolean rainDirectionAllowed,
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

		RainProfile profile = getRainProfile(pitch, speed, verticalSpeed, baseSpawnRate);

		if (!freezeForPause && !canSpawnRain) {
			// Stop spawning immediately but let existing drops expire naturally.
			this.spawnAccumulator = 0.0F;
		}

		if (!freezeForPause && canSpawnRain && profile.spawnRate > 0.0F) {
			this.spawnAccumulator += profile.spawnRate * elapsedSeconds;
			while (this.spawnAccumulator >= 1.0F) {
				spawnOne(width, height, now, profile);
				this.spawnAccumulator -= 1.0F;
			}
		} else if (!freezeForPause) {
			// Pause spawning without clearing active drops so they can fade naturally.
			this.spawnAccumulator = 0.0F;
		}

		for (RainDrop drop : this.drops) {
			if (drop.stream) {
				if (!rainDirectionAllowed) {
					// moving but not looking in movement direction — decelerate to fade out in place
					float decayFactor = Math.max(0.0F, 1.0F - STOP_DECEL_RATE * elapsedSeconds);
					drop.velocityX *= decayFactor;
					drop.velocityY *= decayFactor;
				} else if (canSpawnRain) {
					float streamSpeed = profile.streamSpeedPxPerSec;
					float blend = profile.radialBlend;
					drop.velocityX = drop.radialDirX * streamSpeed * blend;
					drop.velocityY = streamSpeed * (1.0F - blend) + drop.radialDirY * streamSpeed * blend;
					drop.rotation = getStreamRotation(drop.velocityX, drop.velocityY);
				}
			}
			drop.x += drop.velocityX * elapsedSeconds;
			drop.y += drop.velocityY * elapsedSeconds;
		}

		if (!freezeForPause && flying && verticalSpeed > UPWARD_CONVERT_THRESHOLD && speed < CONVERT_MAX_SPEED) {
			float straightFactor = (speed > 0.001D) ? (float) Math.max(0.0D, Math.min(1.0D, verticalSpeed / speed)) : 0.0F;
			float slowFactor = (float) Math.max(0.0D, 1.0D - speed / CONVERT_MAX_SPEED);
			this.convertAccumulator += MAX_CONVERT_RATE * straightFactor * slowFactor * elapsedSeconds;
			while (this.convertAccumulator >= 1.0F) {
				convertRandomStreamToSplash();
				this.convertAccumulator -= 1.0F;
			}
		} else if (!freezeForPause) {
			this.convertAccumulator = 0.0F;
		}

		int removedDrops = this.drops.size();
		this.drops.removeIf(drop -> now - drop.spawnTime > drop.lifetimeMs);
		GlobalParticleCapacity.release(removedDrops - this.drops.size());
		renderDrops(graphics, now);
	}

	private void convertRandomStreamToSplash() {
		List<RainDrop> streamDrops = new ArrayList<>();
		for (RainDrop drop : this.drops) {
			if (drop.stream) streamDrops.add(drop);
		}
		if (streamDrops.isEmpty()) return;
		RainDrop drop = streamDrops.get(random.nextInt(streamDrops.size()));
		RainDropType splashType = pickDropType(SPLASH_TYPES);
		if (splashType == null) {
			return;
		}
		drop.stream = false;
		drop.velocityX = 0.0F;
		drop.velocityY = 0.0F;
		drop.rotation = random.nextFloat() * 20.0F - 10.0F;
		drop.flipX = random.nextBoolean();
		drop.flipY = random.nextBoolean();
		drop.texture = splashType.texture();
		drop.textureSize = splashType.textureSize();
		drop.size = splashType.textureSize() * 2.5F;
	}

	private void renderDrops(GuiGraphics graphics, long now) {
		for (RainDrop drop : this.drops) {
			float age = (now - drop.spawnTime) / (float) drop.lifetimeMs;
			float alpha = 1.0F - age;
			if (alpha <= 0.0F) {
				continue;
			}

			int argb = ParticleVisuals.textureArgb(alpha);

			PoseStack matrices = graphics.pose();
			matrices.pushPose();
			matrices.translate(drop.x, drop.y, 0.0F);

			float half = drop.size / 2.0F;
			float textureHalf = drop.textureSize / 2.0F;
			float drawScale = drop.size / drop.textureSize;
			matrices.translate(half, half, 0.0F);

			matrices.scale(drop.flipX ? -1.0F : 1.0F, drop.flipY ? -1.0F : 1.0F, 1.0F);
			matrices.mulPose(Axis.ZP.rotation((float) Math.toRadians(drop.rotation)));
			matrices.scale(drawScale, drawScale, 1.0F);
			matrices.translate(-textureHalf, -textureHalf, 0.0F);

			graphics.blit(
				
				Objects.requireNonNull(drop.texture),
				0, 0,
				0, 0,
				drop.textureSize, drop.textureSize,
				drop.textureSize, drop.textureSize,
				argb
			);

			matrices.popPose();
		}
	}

	private void spawnOne(int width, int height, long now, RainProfile profile) {
		boolean stream = random.nextFloat() < profile.streamChance;
		RainDropType[] source = stream ? STREAM_TYPES : SPLASH_TYPES;
		RainDropType dropType = pickDropType(source);
		if (dropType == null) {
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
		float size = dropType.textureSize() * 2.5F;
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

			if (overlapsExistingDrop(x, y, size, stream)) {
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

		RainDrop drop = new RainDrop();
		drop.size = size;
		drop.x = x;
		drop.y = y;
		if (stream) {
			float streamSpeed = profile.streamSpeedPxPerSec;
			drop.lifetimeMs = STREAM_LIFETIME_MS;
			drop.stream = true;
			float dropCenterX = x + (size / 2.0F);
			float dropCenterY = y + (size / 2.0F);
			float dx = dropCenterX - centerX;
			float dy = dropCenterY - centerY;
			float len = (float) Math.sqrt(dx * dx + dy * dy);
			if (len > 0.001F) {
				drop.radialDirX = dx / len;
				drop.radialDirY = dy / len;
			} else {
				drop.radialDirX = 0.0F;
				drop.radialDirY = 1.0F;
			}
			// blend radial outward velocity toward straight down as flight speed decreases
			float blend = profile.radialBlend;
			drop.velocityX = drop.radialDirX * streamSpeed * blend;
			drop.velocityY = streamSpeed * (1.0F - blend) + drop.radialDirY * streamSpeed * blend;
			drop.rotation = getStreamRotation(drop.velocityX, drop.velocityY);
			drop.flipX = false;
			drop.flipY = false;
		} else {
			drop.lifetimeMs = SPLASH_LIFETIME_MS;
			drop.velocityX = 0.0F;
			drop.velocityY = 0.0F;
			drop.rotation = random.nextFloat() * 20.0F - 10.0F;
			drop.flipX = random.nextBoolean();
			drop.flipY = random.nextBoolean();
		}
		drop.texture = dropType.texture();
		drop.textureSize = dropType.textureSize();
		drop.spawnTime = now;
		this.drops.add(drop);
		this.lastSpawnTexture = drop.texture;
	}

	private float getStreamRotation(float velocityX, float velocityY) {
		if (Math.abs(velocityX) < 0.001F && Math.abs(velocityY) < 0.001F) {
			return 0.0F;
		}

		return (float) Math.toDegrees(Math.atan2(velocityY, velocityX)) - 90.0F;
	}

	private RainDropType pickDropType(RainDropType[] types) {
		if (types.length == 0) {
			return null;
		}
		List<RainDropType> weightedTypes = new ArrayList<>();
		for (RainDropType type : types) {
			weightedTypes.add(Objects.requireNonNull(type));
		}
		RainDropType candidate = TextureSelection.popRandomAvoidingRepeat(weightedTypes, random, this.lastSpawnTexture, entry -> entry.texture());
		if (candidate != null) {
			return candidate;
		}
		return types[random.nextInt(types.length)];
	}

	private boolean overlapsExistingDrop(float x, float y, float size, boolean stream) {
		float radius = size / 2.0F;
		float centerX = x + radius;
		float centerY = y + radius;
		float overlapPadding = stream ? STREAM_OVERLAP_PADDING : RAIN_DROP_OVERLAP_PADDING;

		for (RainDrop existing : this.drops) {
			float existingRadius = existing.size / 2.0F;
			float existingCenterX = existing.x + existingRadius;
			float existingCenterY = existing.y + existingRadius;

			float dx = centerX - existingCenterX;
			float dy = centerY - existingCenterY;
			float minimumDistance = radius + existingRadius + overlapPadding;
			if (dx * dx + dy * dy < minimumDistance * minimumDistance) {
				return true;
			}
		}

		return false;
	}

	public void clear() {
		GlobalParticleCapacity.release(this.drops.size());
		this.drops.clear();
		this.spawnAccumulator = 0.0F;
		this.convertAccumulator = 0.0F;
		this.lastSpawnTexture = null;
	}

	private RainProfile getRainProfile(float pitch, double speed, double verticalSpeed, float baseSpawnRate) {
		float speedFactor = Math.max(0.0F, Math.min(1.0F, (float) (speed / FLYING_STREAM_FULL_SPEED)));
		float relativeSpeed = (float) Math.max(0.0D, speed / FLYING_STREAM_FULL_SPEED);
		float speedMultiplier = Math.min(
			MAX_FLYING_STREAM_SPEED_MULTIPLIER,
			1.0F + (FLYING_STREAM_SPEED_MULTIPLIER_PER_SPEED * relativeSpeed)
		);
		float streamSpeed = STREAM_SPEED_PX_PER_SEC * speedMultiplier;
		float lookUpDegrees = Math.max(0.0F, -pitch);
		float upFactor = Math.max(0.0F, Math.min(1.0F,
			(lookUpDegrees - RADIAL_UPWARD_START_PITCH) / (90.0F - RADIAL_UPWARD_START_PITCH)));
		upFactor *= upFactor;
		float radialBlend = speedFactor + (1.0F - speedFactor) * upFactor;

		if (pitch > FLYING_START_PITCH) {
			double downwardSpeed = Math.max(0.0D, -verticalSpeed);
			float descentFactor = (float) Math.max(0.0D, Math.min(1.0D,
				(downwardSpeed - DESCENT_STREAM_START_SPEED) / (DESCENT_STREAM_FULL_SPEED - DESCENT_STREAM_START_SPEED)));
			if (descentFactor <= 0.0F) {
				return new RainProfile(0.0F, FLYING_STREAM_MIN_CHANCE, streamSpeed, speedFactor);
			}

			float lookDownFactor = Math.max(0.0F, Math.min(1.0F,
				(pitch - FLYING_START_PITCH) / (DESCENT_LOOK_FULL_PITCH - FLYING_START_PITCH)));
			float spawnRate = baseSpawnRate
				* FLYING_SPAWN_RATE_MULTIPLIER
				* descentFactor
				* (0.65F + 0.35F * lookDownFactor);
			float streamChance = Math.max(FLYING_STREAM_MIN_CHANCE, 0.5F + (0.5F * descentFactor));
			return new RainProfile(spawnRate, streamChance, streamSpeed, speedFactor);
		}

		float lookFactor = Math.max(0.0F, Math.min(1.0F, (FLYING_START_PITCH - pitch) / (FLYING_START_PITCH + 90.0F)));
		float streamChance = Math.max(FLYING_STREAM_MIN_CHANCE, 0.35F + (0.45F * lookFactor) + (0.20F * speedFactor));
		float spawnRate = baseSpawnRate * FLYING_SPAWN_RATE_MULTIPLIER * (0.75F + 0.25F * lookFactor);
		return new RainProfile(spawnRate, streamChance, streamSpeed, radialBlend);
	}

	private record RainProfile(float spawnRate, float streamChance, float streamSpeedPxPerSec, float radialBlend) {}
}