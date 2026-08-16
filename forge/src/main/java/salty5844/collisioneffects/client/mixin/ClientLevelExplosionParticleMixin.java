package salty5844.collisioneffects.client.mixin;

import salty5844.collisioneffects.client.effect.explosion.ExplosionFlashEvents;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
abstract class ClientLevelExplosionParticleMixin {

	@Inject(method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V", at = @At("HEAD"), require = 0)
	private void collisioneffects$recordExplosionParticle(ParticleOptions particle, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, CallbackInfo ci) {
		if (particle.getType() == ParticleTypes.EXPLOSION_EMITTER || particle.getType() == ParticleTypes.EXPLOSION) {
			ExplosionFlashEvents.recordExplosion(x, y, z, System.currentTimeMillis());
		}
	}

	@Inject(method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;", at = @At("HEAD"), require = 0)
	private void collisioneffects$recordExplosionParticleLegacy(ParticleOptions particle, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, CallbackInfoReturnable<?> cir) {
		if (particle.getType() == ParticleTypes.EXPLOSION_EMITTER || particle.getType() == ParticleTypes.EXPLOSION) {
			ExplosionFlashEvents.recordExplosion(x, y, z, System.currentTimeMillis());
		}
	}
}
