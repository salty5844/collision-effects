package salty5844.collisioneffects;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.ModLoadingContext;
import salty5844.collisioneffects.client.config.screen.ConfigScreen;
import salty5844.collisioneffects.client.core.ClientLogic;

@Mod("collision_effects")
public final class ForgeCollisionEffects {

	public ForgeCollisionEffects() {
		ModLoadingContext.get().registerExtensionPoint(
			ConfigScreenHandler.ConfigScreenFactory.class,
			() -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new ConfigScreen(parent))
		);
		ClientLogic.getInstance().init(FMLPaths.CONFIGDIR.get());
	}
}