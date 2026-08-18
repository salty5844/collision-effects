package salty5844.collisioneffects.client.effect.entity;

import salty5844.collisioneffects.client.util.TextureSelection;

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

public final class BeePollen {

	private static final long POLLEN_LIFETIME_MS = 2500L;
	private static final int MAX_SPLATS = 100;
	private static final int MAX_SPAWN_ATTEMPTS = 50;
	private static final float BASE_EXPONENT = 2.0F;
	private static final float SPLAT_OVERLAP_PADDING = 50.0F;
	private static final int SPLATS_PER_TEXTURE = 4;
	private static final long HIT_TRIGGER_COOLDOWN_MS = 400L;
	private static final long TOUCH_MOVE_TRIGGER_COOLDOWN_MS = 150L;
	private static final double TOUCH_MOVE_TRIGGER_DISTANCE_SQR = 0.0009D;

	private record PollenType(ResourceLocation texture, int textureSize, float exponentMultiplier) {}

	private static final PollenType[] POLLEN_TYPES = new PollenType[]{
		new PollenType(new ResourceLocation("collision-effects", "textures/pollen/small-1.png"), 16, 0.75F),
		new PollenType(new ResourceLocation("collision-effects", "textures/pollen/small-2.png"), 16, 0.75F),
		new PollenType(new ResourceLocation("collision-effects", "textures/pollen/small-3.png"), 16, 0.75F),
		new PollenType(new ResourceLocation("collision-effects", "textures/pollen/medium-1.png"), 16, 1.0F),
		new PollenType(new ResourceLocation("collision-effects", "textures/pollen/medium-2.png"), 16, 1.0F),
		new PollenType(new ResourceLocation("collision-effects", "textures/pollen/medium-3.png"), 16, 1.0F),
		new PollenType(new ResourceLocation("collision-effects", "textures/pollen/large-1.png"), 16, 1.25F),
		new PollenType(new ResourceLocation("collision-effects", "textures/pollen/large-2.png"), 16, 1.25F),
		new PollenType(new ResourceLocation("collision-effects", "textures/pollen/large-3.png"), 16, 1.25F)
	};

	private static final class PollenSplat {
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

	private final List<PollenSplat> splats = new ArrayList<>();
	private final Random random = new Random();
	private boolean wasTouchingBee = false;
	private boolean touchTrackingInitialized = false;
	private double lastTouchPlayerX = 0.0D;
	private double lastTouchPlayerY = 0.0D;
	private double lastTouchPlayerZ = 0.0D;
	private double lastTouchBeeX = 0.0D;
	private double lastTouchBeeY = 0.0D;
	private double lastTouchBeeZ = 0.0D;
	private long lastHitSpawnMillis = 0L;
	private long lastTouchMoveSpawnMillis = 0L;
	private ResourceLocation lastSpawnTexture;

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
		Entity touchingBee,
		Entity hitBee
	) {
		if (!enabled || !allowCurrentView) {
			this.clear();
			this.wasTouchingBee = touchingBee != null;
			return;
		}

		boolean currentlyTouchingBee = touchingBee != null;

		if (!freezeForPause) {
			if (touchEnabled && currentlyTouchingBee && !this.wasTouchingBee) {
				spawnAllTypes(width, height, now);
				this.lastTouchMoveSpawnMillis = now;
			}

			if (touchEnabled && currentlyTouchingBee && touchingBee != null) {
				if (!this.touchTrackingInitialized) {
					updateTouchTracking(playerX, playerY, playerZ, touchingBee);
					this.touchTrackingInitialized = true;
				} else {
					boolean playerMoved = distanceSqr(this.lastTouchPlayerX, this.lastTouchPlayerY, this.lastTouchPlayerZ, playerX, playerY, playerZ) > TOUCH_MOVE_TRIGGER_DISTANCE_SQR;
					boolean beeMoved = distanceSqr(this.lastTouchBeeX, this.lastTouchBeeY, this.lastTouchBeeZ, touchingBee.getX(), touchingBee.getY(), touchingBee.getZ()) > TOUCH_MOVE_TRIGGER_DISTANCE_SQR;
					if ((playerMoved || beeMoved) && now - this.lastTouchMoveSpawnMillis >= TOUCH_MOVE_TRIGGER_COOLDOWN_MS) {
						spawnAllTypes(width, height, now);
						this.lastTouchMoveSpawnMillis = now;
					}
					updateTouchTracking(playerX, playerY, playerZ, touchingBee);
				}
			}

			if (!currentlyTouchingBee) {
				this.touchTrackingInitialized = false;
			}

			if (hitEnabled && hitBee != null && now - this.lastHitSpawnMillis >= HIT_TRIGGER_COOLDOWN_MS) {
				spawnAllTypes(width, height, now);
				this.lastHitSpawnMillis = now;
			}
		}

		this.wasTouchingBee = currentlyTouchingBee;
		int removedSplats = this.splats.size();
		this.splats.removeIf(splat -> now - splat.spawnTime > POLLEN_LIFETIME_MS);
		GlobalParticleCapacity.release(removedSplats - this.splats.size());
		renderSplats(graphics, now);
	}

	private void updateTouchTracking(double playerX, double playerY, double playerZ, Entity touchingBee) {
		this.lastTouchPlayerX = playerX;
		this.lastTouchPlayerY = playerY;
		this.lastTouchPlayerZ = playerZ;
		this.lastTouchBeeX = touchingBee.getX();
		this.lastTouchBeeY = touchingBee.getY();
		this.lastTouchBeeZ = touchingBee.getZ();
	}

	private static double distanceSqr(double x1, double y1, double z1, double x2, double y2, double z2) {
		double dx = x1 - x2;
		double dy = y1 - y2;
		double dz = z1 - z2;
		return dx * dx + dy * dy + dz * dz;
	}

	private void spawnAllTypes(int width, int height, long now) {
		List<PollenType> weightedTypes = new ArrayList<>();
		for (PollenType pollenType : POLLEN_TYPES) {
			int weightedRepeats = SPLATS_PER_TEXTURE * ParticleVisuals.filenameSizeWeight(pollenType.texture());
			for (int i = 0; i < weightedRepeats; i++) {
				weightedTypes.add(pollenType);
			}
		}
		while (!weightedTypes.isEmpty()) {
			PollenType pollenType = TextureSelection.popRandomAvoidingRepeat(weightedTypes, random, this.lastSpawnTexture, entry -> entry.texture());
			if (pollenType == null) {
				break;
			}
			spawnOne(width, height, now, pollenType);
		}
	}

	private void spawnOne(int width, int height, long now, PollenType pollenType) {
		if (this.splats.size() >= MAX_SPLATS) {
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
		float size = pollenType.textureSize() * 2.5F;
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

		PollenSplat splat = new PollenSplat();
		splat.size = size;
		splat.rotation = random.nextFloat() * 20.0F - 10.0F;
		splat.flipX = random.nextBoolean();
		splat.flipY = random.nextBoolean();
		splat.x = x;
		splat.y = y;
		splat.texture = pollenType.texture();
		splat.textureSize = pollenType.textureSize();
		splat.spawnTime = now;
		this.splats.add(splat);
		this.lastSpawnTexture = splat.texture;
	}

	private boolean overlapsExistingSplat(float x, float y, float size) {
		float radius = size / 2.0F;
		float centerX = x + radius;
		float centerY = y + radius;

		for (PollenSplat existing : this.splats) {
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
		for (PollenSplat splat : this.splats) {
			float age = (now - splat.spawnTime) / (float) POLLEN_LIFETIME_MS;
			float alpha = 1.0F - age;
			if (alpha <= 0.0F) {
				continue;
			}

			int argb = ParticleVisuals.textureArgb(alpha);

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

			graphics.blit(
				
				Objects.requireNonNull(splat.texture),
				0, 0,
				0, 0,
				splat.textureSize, splat.textureSize,
				splat.textureSize, splat.textureSize,
				argb
			);

			matrices.popPose();
		}
	}

	public void clear() {
		GlobalParticleCapacity.release(this.splats.size());
		this.splats.clear();
		this.lastSpawnTexture = null;
	}
}
