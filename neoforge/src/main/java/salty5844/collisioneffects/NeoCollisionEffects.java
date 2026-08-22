package salty5844.collisioneffects;

import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.ConfigScreenHandler;
import salty5844.collisioneffects.client.config.screen.ConfigScreen;
import salty5844.collisioneffects.client.core.ClientLogic;

@Mod("collision_effects")
public final class NeoCollisionEffects {

	public NeoCollisionEffects() {
		ModLoadingContext.get().registerExtensionPoint(
			ConfigScreenHandler.ConfigScreenFactory.class,
			() -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new ConfigScreen(parent))
		);
		ClientLogic.getInstance().init(FMLPaths.CONFIGDIR.get());
	}
}
