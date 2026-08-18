package salty5844.collisioneffects.client.effect.entity;

import salty5844.collisioneffects.client.util.ParticleVisuals;

import salty5844.collisioneffects.client.util.GlobalParticleCapacity;

import salty5844.collisioneffects.client.config.Config;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public final class ChickenFeathers {

	private static final long FEATHER_LIFETIME_MS = 2500L;
	private static final int MAX_FEATHERS = 100;
	private static final int MAX_SPAWN_ATTEMPTS = 50;
	private static final float BASE_EXPONENT = 2.0F;
	private static final float SPLAT_OVERLAP_PADDING = 50.0F;
	private static final long HIT_TRIGGER_COOLDOWN_MS = 400L;
	private static final long TOUCH_MOVE_TRIGGER_COOLDOWN_MS = 150L;
	private static final double TOUCH_MOVE_TRIGGER_DISTANCE_SQR = 0.0009D;
	private static final int FEATHERS_PER_HIT = 10;

	private record FeatherType(ResourceLocation texture, int textureSize) {}

	private static final FeatherType[] FEATHER_TYPES = new FeatherType[]{
		new FeatherType(new ResourceLocation("collision-effects", "textures/feather/small.png"), 16),
		new FeatherType(new ResourceLocation("collision-effects", "textures/feather/medium.png"), 16),
		new FeatherType(new ResourceLocation("collision-effects", "textures/feather/large.png"), 16)
	};

	private static final class FeatherSplat {
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

	private final List<FeatherSplat> splats = new ArrayList<>();
	private final Random random = new Random();
	private boolean wasTouchingChicken = false;
	private boolean touchTrackingInitialized = false;
	private double lastTouchPlayerX = 0.0D;
	private double lastTouchPlayerY = 0.0D;
	private double lastTouchPlayerZ = 0.0D;
	private double lastTouchChickenX = 0.0D;
	private double lastTouchChickenY = 0.0D;
	private double lastTouchChickenZ = 0.0D;
	private long lastHitSpawnMillis = 0L;
	private long lastTouchMoveSpawnMillis = 0L;

	public void tickAndRender(
		GuiGraphics graphics,
		long now,
		int width,
		int height,
		boolean enabled,
		boolean allowCurrentView,
		boolean freezeForPause,
		boolean touchEnabled,
		boolean hitEnabled,
		double playerX,
		double playerY,
		double playerZ,
		Entity touchingChicken,
		Entity hitChicken
	) {
		if (!enabled || !allowCurrentView) {
			this.clear();
			this.wasTouchingChicken = touchingChicken != null;
			return;
		}

		boolean currentlyTouchingChicken = touchingChicken != null;

		if (!freezeForPause) {
			if (touchEnabled && currentlyTouchingChicken && !this.wasTouchingChicken) {
				spawnFeathers(width, height, now);
				this.lastTouchMoveSpawnMillis = now;
			}

			if (touchEnabled && currentlyTouchingChicken && touchingChicken != null) {
				if (!this.touchTrackingInitialized) {
					updateTouchTracking(playerX, playerY, playerZ, touchingChicken);
					this.touchTrackingInitialized = true;
				} else {
					boolean playerMoved = distanceSqr(this.lastTouchPlayerX, this.lastTouchPlayerY, this.lastTouchPlayerZ, playerX, playerY, playerZ) > TOUCH_MOVE_TRIGGER_DISTANCE_SQR;
					boolean chickenMoved = distanceSqr(this.lastTouchChickenX, this.lastTouchChickenY, this.lastTouchChickenZ, touchingChicken.getX(), touchingChicken.getY(), touchingChicken.getZ()) > TOUCH_MOVE_TRIGGER_DISTANCE_SQR;
					if ((playerMoved || chickenMoved) && now - this.lastTouchMoveSpawnMillis >= TOUCH_MOVE_TRIGGER_COOLDOWN_MS) {
						spawnFeathers(width, height, now);
						this.lastTouchMoveSpawnMillis = now;
					}
					updateTouchTracking(playerX, playerY, playerZ, touchingChicken);
				}
			}

			if (!currentlyTouchingChicken) {
				this.touchTrackingInitialized = false;
			}

			if (hitEnabled && hitChicken != null && now - this.lastHitSpawnMillis >= HIT_TRIGGER_COOLDOWN_MS) {
				spawnFeathers(width, height, now);
				this.lastHitSpawnMillis = now;
			}
		}

		this.wasTouchingChicken = currentlyTouchingChicken;
		int removedSplats = this.splats.size();
		this.splats.removeIf(splat -> now - splat.spawnTime > FEATHER_LIFETIME_MS);
		GlobalParticleCapacity.release(removedSplats - this.splats.size());
		renderSplats(graphics, now);
	}

	private void updateTouchTracking(double playerX, double playerY, double playerZ, Entity touchingChicken) {
		this.lastTouchPlayerX = playerX;
		this.lastTouchPlayerY = playerY;
		this.lastTouchPlayerZ = playerZ;
		this.lastTouchChickenX = touchingChicken.getX();
		this.lastTouchChickenY = touchingChicken.getY();
		this.lastTouchChickenZ = touchingChicken.getZ();
	}

	private static double distanceSqr(double x1, double y1, double z1, double x2, double y2, double z2) {
		double dx = x1 - x2;
		double dy = y1 - y2;
		double dz = z1 - z2;
		return dx * dx + dy * dy + dz * dz;
	}

	private void spawnFeathers(int width, int height, long now) {
		for (int i = 0; i < FEATHERS_PER_HIT; i++) {
			spawnOne(width, height, now);
		}
	}

	private void spawnOne(int width, int height, long now) {
		if (this.splats.size() >= MAX_FEATHERS) {
			return;
		}

		FeatherType featherType = pickFeatherType();
		if (featherType == null) {
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
		float size = featherType.textureSize() * 2.5F;
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

			if (overlapsExistingSplat(x, y, size)) {
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

		FeatherSplat splat = new FeatherSplat();
		splat.size = size;
		splat.rotation = random.nextFloat() * 20.0F - 10.0F;
		splat.flipX = random.nextBoolean();
		splat.flipY = random.nextBoolean();
		splat.x = x;
		splat.y = y;
		splat.texture = featherType.texture();
		splat.textureSize = featherType.textureSize();
		splat.spawnTime = now;
		this.splats.add(splat);
	}

	private FeatherType pickFeatherType() {
		if (FEATHER_TYPES.length == 0) {
			return null;
		}
		List<FeatherType> weightedTypes = new ArrayList<>();
		for (FeatherType featherType : FEATHER_TYPES) {
			for (int i = 0; i < ParticleVisuals.filenameSizeWeight(featherType.texture()); i++) {
				weightedTypes.add(featherType);
			}
		}
		return weightedTypes.get(random.nextInt(weightedTypes.size()));
	}

	private boolean overlapsExistingSplat(float x, float y, float size) {
		float radius = size / 2.0F;
		float centerX = x + radius;
		float centerY = y + radius;

		for (FeatherSplat existing : this.splats) {
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

	private void renderSplats(GuiGraphics graphics, long now) {
		for (FeatherSplat splat : this.splats) {
			float age = (now - splat.spawnTime) / (float) FEATHER_LIFETIME_MS;
			float alpha = 1.0F - age;
			if (alpha <= 0.0F) {
				continue;
			}

			float drawAlpha = ParticleVisuals.textureAlpha(alpha);

			PoseStack matrices = graphics.pose();
			matrices.pushPose();
			matrices.translate(splat.x, splat.y, 0.0F);

			float half = splat.size / 2.0F;
			float textureHalf = splat.textureSize / 2.0F;
			float drawScale = splat.size / splat.textureSize;
			matrices.translate(half, half, 0.0F);

			matrices.scale(splat.flipX ? -1.0F : 1.0F, splat.flipY ? -1.0F : 1.0F, 1.0F);
			matrices.mulPose(Axis.ZP.rotation((float) Math.toRadians(splat.rotation)));
			matrices.scale(drawScale, drawScale, 1.0F);
			matrices.translate(-textureHalf, -textureHalf, 0.0F);

			ParticleVisuals.blitTinted(graphics, Objects.requireNonNull(splat.texture), splat.textureSize, splat.textureSize, drawAlpha);

			matrices.popPose();
		}
	}

	public void clear() {
		GlobalParticleCapacity.release(this.splats.size());
		this.splats.clear();
		this.lastHitSpawnMillis = 0L;
		this.lastTouchMoveSpawnMillis = 0L;
		this.touchTrackingInitialized = false;
		this.wasTouchingChicken = false;
	}
}
