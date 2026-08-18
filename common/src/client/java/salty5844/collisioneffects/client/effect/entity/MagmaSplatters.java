package salty5844.collisioneffects.client.effect.entity;

import salty5844.collisioneffects.client.util.TextureSelection;

import salty5844.collisioneffects.client.util.ParticleVisuals;

import salty5844.collisioneffects.client.util.GlobalParticleCapacity;

import salty5844.collisioneffects.client.config.Config;


import org.joml.Matrix3x2fStack;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class MagmaSplatters {

	private static final long SPLAT_LIFETIME_MS = 2500L;
	private static final int MAX_SPLATS = 100;
	private static final int MAX_SPAWN_ATTEMPTS = 50;
	private static final float BASE_EXPONENT = 2.0F;
	private static final float SPLAT_OVERLAP_PADDING = 50.0F;
	private static final long HIT_TRIGGER_COOLDOWN_MS = 400L;
	private static final long TOUCH_MOVE_TRIGGER_COOLDOWN_MS = 150L;
	private static final double TOUCH_MOVE_TRIGGER_DISTANCE_SQR = 0.0009D;
	private static final float SLIDE_SPEED_PX_PER_SEC = 10.0F;

	private record MagmaType(@NonNull Identifier texture, int textureSize, float exponentMultiplier) {}

	private static final MagmaType[] MAGMA_TYPES = new MagmaType[]{
		new MagmaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/magma/g-small-1.png"), 16, 0.75F),
		new MagmaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/magma/g-small-2.png"), 16, 0.75F),
		new MagmaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/magma/g-small-3.png"), 16, 0.75F),
		new MagmaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/magma/g-small-4.png"), 16, 0.75F),
		new MagmaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/magma/g-medium-1.png"), 16, 1.0F),
		new MagmaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/magma/g-medium-2.png"), 16, 1.0F),
		new MagmaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/magma/g-medium-3.png"), 16, 1.0F),
		new MagmaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/magma/g-medium-4.png"), 16, 1.0F),
		new MagmaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/magma/g-large-1.png"), 16, 1.25F),
		new MagmaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/magma/g-large-2.png"), 16, 1.25F),
		new MagmaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/magma/g-large-3.png"), 16, 1.25F),
		new MagmaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/magma/g-large-4.png"), 16, 1.25F)
	};

	private static final List<MagmaType> WEIGHTED_MAGMA_TYPES =
		ParticleVisuals.buildWeightedPool(MAGMA_TYPES, 2, MagmaType::texture);

	private static final class MagmaSplat {
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

	private final List<MagmaSplat> splats = new ArrayList<>();
	private final Random random = new Random();
	private boolean wasTouchingMagma = false;
	private boolean touchTrackingInitialized = false;
	private double lastTouchPlayerX = 0.0D;
	private double lastTouchPlayerY = 0.0D;
	private double lastTouchPlayerZ = 0.0D;
	private double lastTouchMagmaX = 0.0D;
	private double lastTouchMagmaY = 0.0D;
	private double lastTouchMagmaZ = 0.0D;
	private long lastHitSpawnMillis = 0L;
	private long lastTouchMoveSpawnMillis = 0L;
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
		boolean touchEnabled,
		boolean hitEnabled,
		double playerX,
		double playerY,
		double playerZ,
		@Nullable Entity touchingMagma,
		@Nullable Entity hitMagma
	) {
		if (!enabled || !allowCurrentView) {
			this.clear();
			this.wasTouchingMagma = touchingMagma != null;
			return;
		}

		boolean currentlyTouchingMagma = touchingMagma != null;

		if (!freezeForPause) {
			if (touchEnabled && currentlyTouchingMagma && !this.wasTouchingMagma) {
				spawnAllTypes(width, height, now);
				this.lastTouchMoveSpawnMillis = now;
			}

			if (touchEnabled && currentlyTouchingMagma && touchingMagma != null) {
				if (!this.touchTrackingInitialized) {
					updateTouchTracking(playerX, playerY, playerZ, touchingMagma);
					this.touchTrackingInitialized = true;
				} else {
					boolean playerMoved = distanceSqr(this.lastTouchPlayerX, this.lastTouchPlayerY, this.lastTouchPlayerZ, playerX, playerY, playerZ) > TOUCH_MOVE_TRIGGER_DISTANCE_SQR;
					boolean magmaMoved = distanceSqr(this.lastTouchMagmaX, this.lastTouchMagmaY, this.lastTouchMagmaZ, touchingMagma.getX(), touchingMagma.getY(), touchingMagma.getZ()) > TOUCH_MOVE_TRIGGER_DISTANCE_SQR;
					if ((playerMoved || magmaMoved) && now - this.lastTouchMoveSpawnMillis >= TOUCH_MOVE_TRIGGER_COOLDOWN_MS) {
						spawnAllTypes(width, height, now);
						this.lastTouchMoveSpawnMillis = now;
					}
					updateTouchTracking(playerX, playerY, playerZ, touchingMagma);
				}
			}

			if (!currentlyTouchingMagma) {
				this.touchTrackingInitialized = false;
			}

			if (hitEnabled && hitMagma != null && now - this.lastHitSpawnMillis >= HIT_TRIGGER_COOLDOWN_MS) {
				spawnAllTypes(width, height, now);
				this.lastHitSpawnMillis = now;
			}
		}

		if (!freezeForPause) {
			for (MagmaSplat splat : this.splats) {
				splat.y += SLIDE_SPEED_PX_PER_SEC * elapsedSeconds;
			}
		}

		this.wasTouchingMagma = currentlyTouchingMagma;
		int removedSplats = this.splats.size();
		this.splats.removeIf(splat -> now - splat.spawnTime > SPLAT_LIFETIME_MS);
		GlobalParticleCapacity.release(removedSplats - this.splats.size());
		renderSplats(graphics, now);
	}

	private void updateTouchTracking(double playerX, double playerY, double playerZ, Entity touchingMagma) {
		this.lastTouchPlayerX = playerX;
		this.lastTouchPlayerY = playerY;
		this.lastTouchPlayerZ = playerZ;
		this.lastTouchMagmaX = touchingMagma.getX();
		this.lastTouchMagmaY = touchingMagma.getY();
		this.lastTouchMagmaZ = touchingMagma.getZ();
	}

	private static double distanceSqr(double x1, double y1, double z1, double x2, double y2, double z2) {
		double dx = x1 - x2;
		double dy = y1 - y2;
		double dz = z1 - z2;
		return dx * dx + dy * dy + dz * dz;
	}

	private void spawnAllTypes(int width, int height, long now) {
		List<MagmaType> doubledTypes = new ArrayList<>(WEIGHTED_MAGMA_TYPES);
		while (!doubledTypes.isEmpty()) {
			MagmaType magmaType = TextureSelection.popRandomAvoidingRepeat(doubledTypes, random, this.lastSpawnTexture, entry -> entry.texture());
			if (magmaType == null) {
				break;
			}
			spawnOne(width, height, now, magmaType);
		}
	}

	private void spawnOne(int width, int height, long now, MagmaType magmaType) {
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
		float size = magmaType.textureSize() * 2.5F;
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

		MagmaSplat splat = new MagmaSplat();
		splat.size = size;
		splat.rotation = random.nextFloat() * 20.0F - 10.0F;
		splat.flipX = random.nextBoolean();
		splat.flipY = random.nextBoolean();
		splat.x = x;
		splat.y = y;
		splat.texture = magmaType.texture();
		splat.textureSize = magmaType.textureSize();
		splat.spawnTime = now;
		this.splats.add(splat);
		this.lastSpawnTexture = splat.texture;
	}

	private boolean overlapsExistingSplat(float x, float y, float size) {
		float radius = size / 2.0F;
		float centerX = x + radius;
		float centerY = y + radius;

		for (MagmaSplat existing : this.splats) {
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

	private void renderSplats(GuiGraphicsExtractor graphics, long now) {
		for (MagmaSplat splat : this.splats) {
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

	public void clear() {
		GlobalParticleCapacity.release(this.splats.size());
		this.splats.clear();
		this.wasTouchingMagma = false;
		this.touchTrackingInitialized = false;
		this.lastSpawnTexture = null;
	}
}
