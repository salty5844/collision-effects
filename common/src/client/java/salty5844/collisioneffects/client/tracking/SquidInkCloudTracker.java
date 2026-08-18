package salty5844.collisioneffects.client.tracking;


import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class SquidInkCloudTracker {

	private static final long CLOUD_LIFETIME_MS = 1400L;
	private static final double CLOUD_RADIUS = 2.25D;
	private static final double SQUID_SCAN_RADIUS = 8.5D;

	private record InkCloud(double x, double y, double z, long spawnTime) {}

	private static final List<InkCloud> RECENT_CLOUDS = new ArrayList<>();
	private static final Map<UUID, Integer> LAST_HURT_TICKS = new HashMap<>();

	private SquidInkCloudTracker() {
	}

	public static void tickNearbySquids(Minecraft client, LocalPlayer player, long now, boolean freezeForPause) {
		if (client == null || player == null || client.level == null) {
			trimExpired(now);
			return;
		}
		var level = Objects.requireNonNull(client.level);

		if (!freezeForPause) {
			Map<UUID, Integer> current = new HashMap<>();
			for (Entity entity : level.getEntities(player, player.getBoundingBox().inflate(SQUID_SCAN_RADIUS))) {
				if (!(entity instanceof LivingEntity living) || !isSquid(entity)) {
					continue;
				}

				UUID squidId = entity.getUUID();
				int hurtTicks = living.hurtTime;
				int previous = LAST_HURT_TICKS.getOrDefault(squidId, 0);
				if (hurtTicks > 0 && previous <= 0) {
					registerCloud(entity.getX(), entity.getY() + 0.6D, entity.getZ(), now);
				}
				current.put(squidId, hurtTicks);
			}
			LAST_HURT_TICKS.clear();
			LAST_HURT_TICKS.putAll(current);
		}

		trimExpired(now);
	}

	private static void registerCloud(double x, double y, double z, long now) {
		RECENT_CLOUDS.add(new InkCloud(x, y, z, now));
		trimExpired(now);
	}

	private static boolean isSquid(Entity entity) {
		ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
		String path = entityId.getPath();
		return path.contains("squid") && !path.contains("ink");
	}

	public static boolean isPlayerInInk(LocalPlayer player, long now) {
		if (player == null || !player.isInWater()) {
			trimExpired(now);
			return false;
		}

		trimExpired(now);
		double px = player.getX();
		double py = player.getY() + 0.6D;
		double pz = player.getZ();
		double radiusSquared = CLOUD_RADIUS * CLOUD_RADIUS;

		for (InkCloud cloud : RECENT_CLOUDS) {
			double dx = px - cloud.x;
			double dy = py - cloud.y;
			double dz = pz - cloud.z;
			double distanceSquared = dx * dx + dy * dy + dz * dz;
			if (distanceSquared <= radiusSquared) {
				return true;
			}
		}

		return false;
	}

	public static void clear() {
		RECENT_CLOUDS.clear();
		LAST_HURT_TICKS.clear();
	}

	private static void trimExpired(long now) {
		Iterator<InkCloud> iterator = RECENT_CLOUDS.iterator();
		while (iterator.hasNext()) {
			InkCloud cloud = iterator.next();
			if (now - cloud.spawnTime > CLOUD_LIFETIME_MS) {
				iterator.remove();
			}
		}
	}
}