package salty5844.collisioneffects;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import salty5844.collisioneffects.client.NeoClientSetup;

@Mod("collision_effects")
public class NeoCollisionEffects {

	public NeoCollisionEffects(IEventBus modEventBus, ModContainer modContainer) {
		NeoClientSetup.register(modEventBus, modContainer);
	}
}
