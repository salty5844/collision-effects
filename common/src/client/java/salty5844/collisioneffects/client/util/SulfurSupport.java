package salty5844.collisioneffects.client.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import java.util.Objects;

// Sulfur cubes only exist from Minecraft 26.2 onward, so availability is resolved from the registry at runtime.
public final class SulfurSupport {

	private static final Identifier SULFUR_CUBE_ID = Objects.requireNonNull(Identifier.fromNamespaceAndPath("minecraft", "sulfur_cube"));

	private SulfurSupport() {}

	public static boolean isAvailable() {
		return BuiltInRegistries.ENTITY_TYPE.containsKey(SULFUR_CUBE_ID);
	}

	public static boolean isSulfurCube(Entity entity) {
		Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
		return id != null && id.getPath().contains("sulfur_cube");
	}
}
