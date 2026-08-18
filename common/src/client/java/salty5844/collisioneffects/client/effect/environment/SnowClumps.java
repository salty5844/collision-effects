package salty5844.collisioneffects.client.effect.environment;

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
import java.util.UUID;

public final class SnowClumps {

	private static final long CLUMP_LIFETIME_MS = 2500L;
	private static final int MAX_CLUMPS = 100;
	private static final int MAX_SPAWN_ATTEMPTS = 50;
	private static final float BASE_EXPONENT = 2.0F;
	private static final float SPLAT_OVERLAP_PADDING = 50.0F;
	private static final int CLUMPS_PER_TEXTURE = 8;
	private static final float SLIDE_SPEED_PX_PER_SEC = 10.0F;

	private record SnowClumpType(ResourceLocation texture, int textureSize, float exponentMultiplier) {}

	private static final SnowClumpType[] CLUMP_TYPES = new SnowClumpType[]{
		new SnowClumpType(new ResourceLocation("collision-effects", "textures/clumps/small-1.png"), 16, 1.0F),
		new SnowClumpType(new ResourceLocation("collision-effects", "textures/clumps/small-2.png"), 16, 1.0F),
		new SnowClumpType(new ResourceLocation("collision-effects", "textures/clumps/small-3.png"), 16, 1.0F),
		new SnowClumpType(new ResourceLocation("collision-effects", "textures/clumps/medium-1.png"), 16, 1.25F),
		new SnowClumpType(new ResourceLocation("collision-effects", "textures/clumps/medium-2.png"), 16, 1.25F),
		new SnowClumpType(new ResourceLocation("collision-effects", "textures/clumps/medium-3.png"), 16, 1.25F),
		new SnowClumpType(new ResourceLocation("collision-effects", "textures/clumps/large-1.png"), 16, 1.5F),
		new SnowClumpType(new ResourceLocation("collision-effects", "textures/clumps/large-2.png"), 16, 1.5F),
		new SnowClumpType(new ResourceLocation("collision-effects", "textures/clumps/large-3.png"), 16, 1.5F)
	};

	private static final List<SnowClumpType> WEIGHTED_CLUMP_TYPES =
		ParticleVisuals.buildWeightedPool(CLUMP_TYPES, CLUMPS_PER_TEXTURE, SnowClumpType::texture);

	private static final class SnowClump {
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

	private final List<SnowClump> clumps = new ArrayList<>();
	private final Random random = new Random();
	private UUID lastHitSnowballId;
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
		Entity hitSnowball
	) {
		if (!enabled || !allowCurrentView) {
			this.clear();
			return;
		}

		if (!freezeForPause && hitSnowball != null) {
			UUID hitId = hitSnowball.getUUID();
			boolean newProjectile = !Objects.equals(this.lastHitSnowballId, hitId);
			if (newProjectile) {
				spawnAllTypes(width, height, now);
				this.lastHitSnowballId = hitId;
			}
		}

		if (!freezeForPause) {
			for (SnowClump clump : this.clumps) {
				clump.y += SLIDE_SPEED_PX_PER_SEC * elapsedSeconds;
			}
		}

		int removedClumps = this.clumps.size();
		this.clumps.removeIf(clump -> now - clump.spawnTime > CLUMP_LIFETIME_MS);
		GlobalParticleCapacity.release(removedClumps - this.clumps.size());
		renderClumps(graphics, now);
	}

	private void spawnAllTypes(int width, int height, long now) {
		List<SnowClumpType> weightedTypes = new ArrayList<>(WEIGHTED_CLUMP_TYPES);
		while (!weightedTypes.isEmpty()) {
			SnowClumpType clumpType = TextureSelection.popRandomAvoidingRepeat(weightedTypes, random, this.lastSpawnTexture, entry -> entry.texture());
			if (clumpType == null) {
				break;
			}
			spawnOne(width, height, now, clumpType);
		}
	}

	private void spawnOne(int width, int height, long now, SnowClumpType clumpType) {
		if (this.clumps.size() >= MAX_CLUMPS) {
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
		float size = clumpType.textureSize() * 2.5F;
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

			if (overlapsExistingClump(x, y, size)) {
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

		SnowClump clump = new SnowClump();
		clump.size = size;
		clump.rotation = random.nextFloat() * 20.0F - 10.0F;
		clump.flipX = random.nextBoolean();
		clump.flipY = random.nextBoolean();
		clump.x = x;
		clump.y = y;
		clump.texture = clumpType.texture();
		clump.textureSize = clumpType.textureSize();
		clump.spawnTime = now;
		this.clumps.add(clump);
		this.lastSpawnTexture = clump.texture;
	}

	private boolean overlapsExistingClump(float x, float y, float size) {
		float radius = size / 2.0F;
		float centerX = x + radius;
		float centerY = y + radius;

		for (SnowClump existing : this.clumps) {
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

	private void renderClumps(GuiGraphics graphics, long now) {
		for (SnowClump clump : this.clumps) {
			float age = (now - clump.spawnTime) / (float) CLUMP_LIFETIME_MS;
			float alpha = 1.0F - age;
			if (alpha <= 0.0F) {
				continue;
			}

			float drawAlpha = ParticleVisuals.textureAlpha(alpha);

			PoseStack matrices = graphics.pose();
			matrices.pushPose();
			matrices.translate(clump.x, clump.y, 0.0F);

			float half = clump.size / 2.0F;
			float textureHalf = clump.textureSize / 2.0F;
			float drawScale = clump.size / clump.textureSize;
			matrices.translate(half, half, 0.0F);

			matrices.scale(clump.flipX ? -1.0F : 1.0F, clump.flipY ? -1.0F : 1.0F, 1.0F);
			matrices.mulPose(Axis.ZP.rotation((float) Math.toRadians(clump.rotation)));
			matrices.scale(drawScale, drawScale, 1.0F);
			matrices.translate(-textureHalf, -textureHalf, 0.0F);

			ParticleVisuals.blitTinted(graphics, Objects.requireNonNull(clump.texture), clump.textureSize, clump.textureSize, drawAlpha);

			matrices.popPose();
		}
	}

	public void clear() {
		GlobalParticleCapacity.release(this.clumps.size());
		this.clumps.clear();
		this.lastHitSnowballId = null;
		this.lastSpawnTexture = null;
	}
}