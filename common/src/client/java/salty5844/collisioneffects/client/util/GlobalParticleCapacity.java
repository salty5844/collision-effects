package salty5844.collisioneffects.client.util;

import salty5844.collisioneffects.client.config.Config;


public final class GlobalParticleCapacity {

	private static int activeCount = 0;

	private GlobalParticleCapacity() {
	}

	public static boolean tryAcquire() {
		int capacity = Config.getInstance().getParticleCapacity();
		if (capacity <= 0) {
			return false;
		}
		if (activeCount >= capacity) {
			return false;
		}
		activeCount++;
		return true;
	}

	public static void release(int count) {
		if (count <= 0) {
			return;
		}
		activeCount = Math.max(0, activeCount - count);
	}

	public static void reset() {
		activeCount = 0;
	}
}