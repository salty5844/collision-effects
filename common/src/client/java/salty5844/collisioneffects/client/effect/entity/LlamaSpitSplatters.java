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
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public final class LlamaSpitSplatters {

	private static final long SPLAT_LIFETIME_MS = 2500L;
	private static final int MAX_SPLATS = 100;
	private static final int MAX_SPAWN_ATTEMPTS = 50;
	private static final float BASE_EXPONENT = 2.0F;
	private static final float SPLAT_OVERLAP_PADDING = 50.0F;
	private static final int SPLATS_PER_TEXTURE = 5;
	private static final float SLIDE_SPEED_PX_PER_SEC = 10.0F;

	private record LlamaType(@NonNull Identifier texture, int textureSize, float exponentMultiplier) {}

	private static final LlamaType[] LLAMA_TYPES = new LlamaType[]{
		new LlamaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/llama/g-small-1.png"), 16, 0.75F),
		new LlamaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/llama/g-small-2.png"), 16, 0.75F),
		new LlamaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/llama/g-small-3.png"), 16, 0.75F),
		new LlamaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/llama/g-small-4.png"), 16, 0.75F),
		new LlamaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/llama/g-medium-1.png"), 16, 1.0F),
		new LlamaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/llama/g-medium-2.png"), 16, 1.0F),
		new LlamaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/llama/g-medium-3.png"), 16, 1.0F),
		new LlamaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/llama/g-medium-4.png"), 16, 1.0F),
		new LlamaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/llama/g-large-1.png"), 16, 1.25F),
		new LlamaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/llama/g-large-2.png"), 16, 1.25F),
		new LlamaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/llama/g-large-3.png"), 16, 1.25F),
		new LlamaType(Identifier.fromNamespaceAndPath("collision-effects", "textures/llama/g-large-4.png"), 16, 1.25F)
	};

	private static final class LlamaSplat {
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

	private final List<LlamaSplat> splats = new ArrayList<>();
	private final Random random = new Random();
	private @Nullable UUID lastHitSpitId;
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
		@Nullable Entity hitLlamaSpit
	) {
		if (!enabled || !allowCurrentView) {
			this.clear();
			return;
		}

		if (!freezeForPause && hitLlamaSpit != null) {
			UUID hitId = hitLlamaSpit.getUUID();
			if (!Objects.equals(this.lastHitSpitId, hitId)) {
				spawnAllTypes(width, height, now);
				this.lastHitSpitId = hitId;
			}
		}

		if (!freezeForPause) {
			for (LlamaSplat splat : this.splats) {
				splat.y += SLIDE_SPEED_PX_PER_SEC * elapsedSeconds;
			}
		}

		int removedSplats = this.splats.size();
		this.splats.removeIf(splat -> now - splat.spawnTime > SPLAT_LIFETIME_MS);
		GlobalParticleCapacity.release(removedSplats - this.splats.size());
		renderSplats(graphics, now);
	}

	private void spawnAllTypes(int width, int height, long now) {
		List<LlamaType> weightedTypes = new ArrayList<>();
		for (LlamaType llamaType : LLAMA_TYPES) {
			int weightedRepeats = SPLATS_PER_TEXTURE * ParticleVisuals.filenameSizeWeight(llamaType.texture());
			for (int i = 0; i < weightedRepeats; i++) {
				weightedTypes.add(llamaType);
			}
		}
		while (!weightedTypes.isEmpty()) {
			LlamaType llamaType = TextureSelection.popRandomAvoidingRepeat(weightedTypes, random, this.lastSpawnTexture, entry -> entry.texture());
			if (llamaType == null) {
				break;
			}
			spawnOne(width, height, now, llamaType);
		}
	}

	private void spawnOne(int width, int height, long now, LlamaType llamaType) {
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
		float size = llamaType.textureSize() * 2.5F;
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

		LlamaSplat splat = new LlamaSplat();
		splat.size = size;
		splat.rotation = random.nextFloat() * 20.0F - 10.0F;
		splat.flipX = random.nextBoolean();
		splat.flipY = random.nextBoolean();
		splat.x = x;
		splat.y = y;
		splat.texture = llamaType.texture();
		splat.textureSize = llamaType.textureSize();
		splat.spawnTime = now;
		this.splats.add(splat);
		this.lastSpawnTexture = splat.texture;
	}

	private boolean overlapsExistingSplat(float x, float y, float size) {
		float radius = size / 2.0F;
		float centerX = x + radius;
		float centerY = y + radius;

		for (LlamaSplat existing : this.splats) {
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
		for (LlamaSplat splat : this.splats) {
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
		this.lastHitSpitId = null;
		this.lastSpawnTexture = null;
	}
}