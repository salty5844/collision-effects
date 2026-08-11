package salty5844.collisioneffects.client.util;


import java.util.function.IntSupplier;

public final class ParticleCapacityLimiter {

	private static IntSupplier currentCountSupplier = () -> 0;
	private static IntSupplier capacitySupplier = () -> 100;

	private ParticleCapacityLimiter() {
	}

	public static void configure(IntSupplier currentCountSupplier, IntSupplier capacitySupplier) {
		ParticleCapacityLimiter.currentCountSupplier = currentCountSupplier;
		ParticleCapacityLimiter.capacitySupplier = capacitySupplier;
	}

	public static boolean canSpawn() {
		int capacity = Math.max(0, capacitySupplier.getAsInt());
		if (capacity == 0) {
			return false;
		}
		return Math.max(0, currentCountSupplier.getAsInt()) < capacity;
	}
}
