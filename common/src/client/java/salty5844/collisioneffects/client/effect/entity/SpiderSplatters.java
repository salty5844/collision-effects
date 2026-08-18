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

public final class SpiderSplatters {

	private static final long SPLAT_LIFETIME_MS = 2500L;
	private static final int MAX_SPLATS = 100;
	private static final int MAX_SPAWN_ATTEMPTS = 50;
	private static final float BASE_EXPONENT = 2.0F;
	private static final float SPLAT_OVERLAP_PADDING = 50.0F;
	private static final long HIT_TRIGGER_COOLDOWN_MS = 400L;
	private static final int SPLATS_PER_TEXTURE = 4;
	private static final float SLIDE_SPEED_PX_PER_SEC = 10.0F;

	private record SpiderType(ResourceLocation texture, int textureSize, float exponentMultiplier) {}

	private static final SpiderType[] SPIDER_TYPES = new SpiderType[]{
		new SpiderType(new ResourceLocation("collision-effects", "textures/spider/g-small-1.png"), 16, 0.75F),
		new SpiderType(new ResourceLocation("collision-effects", "textures/spider/g-small-2.png"), 16, 0.75F),
		new SpiderType(new ResourceLocation("collision-effects", "textures/spider/g-small-3.png"), 16, 0.75F),
		new SpiderType(new ResourceLocation("collision-effects", "textures/spider/g-small-4.png"), 16, 0.75F),
		new SpiderType(new ResourceLocation("collision-effects", "textures/spider/g-medium-1.png"), 16, 1.0F),
		new SpiderType(new ResourceLocation("collision-effects", "textures/spider/g-medium-2.png"), 16, 1.0F),
		new SpiderType(new ResourceLocation("collision-effects", "textures/spider/g-medium-3.png"), 16, 1.0F),
		new SpiderType(new ResourceLocation("collision-effects", "textures/spider/g-medium-4.png"), 16, 1.0F),
		new SpiderType(new ResourceLocation("collision-effects", "textures/spider/g-large-1.png"), 16, 1.25F),
		new SpiderType(new ResourceLocation("collision-effects", "textures/spider/g-large-2.png"), 16, 1.25F),
		new SpiderType(new ResourceLocation("collision-effects", "textures/spider/g-large-3.png"), 16, 1.25F),
		new SpiderType(new ResourceLocation("collision-effects", "textures/spider/g-large-4.png"), 16, 1.25F)
	};

	private static final List<SpiderType> WEIGHTED_SPIDER_TYPES =
		ParticleVisuals.buildWeightedPool(SPIDER_TYPES, SPLATS_PER_TEXTURE, SpiderType::texture);

	private static final class SpiderSplat {
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

	private final List<SpiderSplat> splats = new ArrayList<>();
	private final Random random = new Random();
	private long lastHitSpawnMillis = 0L;
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
		Entity hitSpider
	) {
		if (!enabled || !allowCurrentView) {
			this.clear();
			return;
		}

		if (!freezeForPause && hitSpider != null && now - this.lastHitSpawnMillis >= HIT_TRIGGER_COOLDOWN_MS) {
			spawnAllTypes(width, height, now);
			this.lastHitSpawnMillis = now;
		}

		if (!freezeForPause) {
			for (SpiderSplat splat : this.splats) {
				splat.y += SLIDE_SPEED_PX_PER_SEC * elapsedSeconds;
			}
		}

		int removedSplats = this.splats.size();
		this.splats.removeIf(splat -> now - splat.spawnTime > SPLAT_LIFETIME_MS);
		GlobalParticleCapacity.release(removedSplats - this.splats.size());
		renderSplats(graphics, now);
	}

	private void spawnAllTypes(int width, int height, long now) {
		List<SpiderType> weightedTypes = new ArrayList<>(WEIGHTED_SPIDER_TYPES);
		while (!weightedTypes.isEmpty()) {
			SpiderType spiderType = TextureSelection.popRandomAvoidingRepeat(weightedTypes, random, this.lastSpawnTexture, entry -> entry.texture());
			if (spiderType == null) {
				break;
			}
			spawnOne(width, height, now, spiderType);
		}
	}

	private void spawnOne(int width, int height, long now, SpiderType spiderType) {
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
		float size = spiderType.textureSize() * 2.5F;
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

		SpiderSplat splat = new SpiderSplat();
		splat.size = size;
		splat.rotation = random.nextFloat() * 20.0F - 10.0F;
		splat.flipX = random.nextBoolean();
		splat.flipY = random.nextBoolean();
		splat.x = x;
		splat.y = y;
		splat.texture = spiderType.texture();
		splat.textureSize = spiderType.textureSize();
		splat.spawnTime = now;
		this.splats.add(splat);
		this.lastSpawnTexture = splat.texture;
	}

	private boolean overlapsExistingSplat(float x, float y, float size) {
		float radius = size / 2.0F;
		float centerX = x + radius;
		float centerY = y + radius;

		for (SpiderSplat existing : this.splats) {
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
		for (SpiderSplat splat : this.splats) {
			float age = (now - splat.spawnTime) / (float) SPLAT_LIFETIME_MS;
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
		this.lastSpawnTexture = null;
	}
}
