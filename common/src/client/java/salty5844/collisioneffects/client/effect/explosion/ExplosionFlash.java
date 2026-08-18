package salty5844.collisioneffects.client.effect.explosion;

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

public final class ExplosionFlash {

	private static final long WHITE_FADE_MS = 2500L;
	private static final long SOOT_FADE_MS = 5000L;
	private static final int MAX_SOOT = 100;
	private static final int MAX_SPAWN_ATTEMPTS = 50;
	private static final float BASE_EXPONENT = 2.0F;
	private static final float SOOT_OVERLAP_PADDING = 25.0F;
	private static final int SOOT_PER_TEXTURE = 2;

	private record SootType(ResourceLocation texture, int textureSize, float exponentMultiplier) {}

	private static final SootType[] SOOT_TYPES = new SootType[]{
		new SootType(new ResourceLocation("collision-effects", "textures/explosion/small-1.png"), 16, 0.75F),
		new SootType(new ResourceLocation("collision-effects", "textures/explosion/small-2.png"), 16, 0.75F),
		new SootType(new ResourceLocation("collision-effects", "textures/explosion/small-3.png"), 16, 0.75F),
		new SootType(new ResourceLocation("collision-effects", "textures/explosion/medium-1.png"), 16, 1.0F),
		new SootType(new ResourceLocation("collision-effects", "textures/explosion/medium-2.png"), 16, 1.0F),
		new SootType(new ResourceLocation("collision-effects", "textures/explosion/medium-3.png"), 16, 1.0F),
		new SootType(new ResourceLocation("collision-effects", "textures/explosion/large-1.png"), 16, 1.25F),
		new SootType(new ResourceLocation("collision-effects", "textures/explosion/large-2.png"), 16, 1.25F),
		new SootType(new ResourceLocation("collision-effects", "textures/explosion/large-3.png"), 16, 1.25F)
	};

	private static final class SootSplat {
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

	private final List<SootSplat> sootSplats = new ArrayList<>();
	private final Random random = new Random();
	private long whiteFlashStartMillis = -1L;
	private ResourceLocation lastSpawnTexture;

	public void tickAndRender(
		GuiGraphics graphics,
		long now,
		int width,
		int height,
		boolean flashEnabled,
		boolean debrisEnabled,
		boolean allowCurrentView,
		boolean freezeForPause,
		boolean explosionTriggered
	) {
		if (!allowCurrentView || (!flashEnabled && !debrisEnabled)) {
			clear();
			return;
		}

		if (!freezeForPause && explosionTriggered) {
			if (flashEnabled) {
				whiteFlashStartMillis = now;
			} else {
				whiteFlashStartMillis = -1L;
			}
			if (debrisEnabled) {
				spawnAllSoot(width, height, now);
			}
		}

		if (debrisEnabled) {
			int removedSoot = sootSplats.size();
			sootSplats.removeIf(splat -> now - splat.spawnTime > SOOT_FADE_MS);
			GlobalParticleCapacity.release(removedSoot - sootSplats.size());
			renderSoot(graphics, now);
		} else {
			GlobalParticleCapacity.release(sootSplats.size());
			sootSplats.clear();
		}

		if (flashEnabled) {
			renderWhiteFlash(graphics, now, width, height);
		} else {
			whiteFlashStartMillis = -1L;
		}
	}

	public void clear() {
		GlobalParticleCapacity.release(sootSplats.size());
		sootSplats.clear();
		whiteFlashStartMillis = -1L;
		this.lastSpawnTexture = null;
	}

	private void spawnAllSoot(int width, int height, long now) {
		List<SootType> burstTypes = new ArrayList<>();
		for (SootType sootType : SOOT_TYPES) {
			int weightedRepeats = SOOT_PER_TEXTURE * ParticleVisuals.filenameSizeWeight(sootType.texture());
			for (int i = 0; i < weightedRepeats; i++) {
				burstTypes.add(sootType);
			}
		}
		while (!burstTypes.isEmpty()) {
			SootType sootType = TextureSelection.popRandomAvoidingRepeat(burstTypes, random, this.lastSpawnTexture, entry -> entry.texture());
			if (sootType == null) {
				break;
			}
			spawnOne(width, height, now, sootType);
		}
	}

	private void spawnOne(int width, int height, long now, SootType sootType) {
		if (sootSplats.size() >= MAX_SOOT) {
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
		float size = sootType.textureSize() * 2.5F;
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

			if (overlapsExistingSoot(x, y, size)) {
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

		SootSplat splat = new SootSplat();
		splat.size = size;
		splat.rotation = random.nextFloat() * 20.0F - 10.0F;
		splat.flipX = random.nextBoolean();
		splat.flipY = random.nextBoolean();
		splat.x = x;
		splat.y = y;
		splat.texture = sootType.texture();
		splat.textureSize = sootType.textureSize();
		splat.spawnTime = now;
		sootSplats.add(splat);
		this.lastSpawnTexture = splat.texture;
	}

	private boolean overlapsExistingSoot(float x, float y, float size) {
		float radius = size / 2.0F;
		float centerX = x + radius;
		float centerY = y + radius;

		for (SootSplat existing : sootSplats) {
			float existingRadius = existing.size / 2.0F;
			float existingCenterX = existing.x + existingRadius;
			float existingCenterY = existing.y + existingRadius;

			float dx = centerX - existingCenterX;
			float dy = centerY - existingCenterY;
			float minimumDistance = radius + existingRadius + SOOT_OVERLAP_PADDING;
			if (dx * dx + dy * dy < minimumDistance * minimumDistance) {
				return true;
			}
		}

		return false;
	}

	private void renderSoot(GuiGraphics graphics, long now) {
		for (SootSplat splat : sootSplats) {
			long ageMs = now - splat.spawnTime;
			if (ageMs >= SOOT_FADE_MS) {
				continue;
			}

			float alpha = 1.0F - (ageMs / (float) SOOT_FADE_MS);
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

	private void renderWhiteFlash(GuiGraphics graphics, long now, int width, int height) {
		if (whiteFlashStartMillis < 0L || width <= 0 || height <= 0) {
			return;
		}

		long ageMs = now - whiteFlashStartMillis;
		if (ageMs >= WHITE_FADE_MS) {
			return;
		}

		float alpha = 1.0F - (ageMs / (float) WHITE_FADE_MS);
		int argb = ((int) (Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F) << 24) | 0x00FFFFFF;
		graphics.fill(0, 0, width, height, argb);
	}
}
