package salty5844.collisioneffects.client.effect.explosion;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ExplosionFlashEvents {

	public record ExplosionEvent(double x, double y, double z, long timestampMillis) {}

	private static final List<ExplosionEvent> EVENTS = new ArrayList<>();
	private static final long DUPLICATE_WINDOW_MS = 80L;
	private static final double DUPLICATE_DISTANCE_SQR = 0.25D;

	private ExplosionFlashEvents() {
	}

	public static synchronized void recordExplosion(double x, double y, double z, long timestampMillis) {
		for (ExplosionEvent existing : EVENTS) {
			if (timestampMillis - existing.timestampMillis <= DUPLICATE_WINDOW_MS
				&& distanceSqr(existing.x, existing.y, existing.z, x, y, z) <= DUPLICATE_DISTANCE_SQR) {
				return;
			}
		}
		EVENTS.add(new ExplosionEvent(x, y, z, timestampMillis));
	}

	public static synchronized List<ExplosionEvent> drainRecent(long now, long maxAgeMs) {
		List<ExplosionEvent> recent = new ArrayList<>();
		Iterator<ExplosionEvent> iterator = EVENTS.iterator();
		while (iterator.hasNext()) {
			ExplosionEvent event = iterator.next();
			long age = now - event.timestampMillis;
			if (age > maxAgeMs) {
				iterator.remove();
				continue;
			}
			recent.add(event);
			iterator.remove();
		}
		return recent;
	}

	public static synchronized void clear() {
		EVENTS.clear();
	}

	private static double distanceSqr(double ax, double ay, double az, double bx, double by, double bz) {
		double dx = ax - bx;
		double dy = ay - by;
		double dz = az - bz;
		return dx * dx + dy * dy + dz * dz;
	}
}
