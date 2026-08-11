package salty5844.collisioneffects.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import salty5844.collisioneffects.client.config.screen.ConfigScreen;
import salty5844.collisioneffects.client.core.ClientLogic;

public final class NeoClientSetup {

	public static void register(IEventBus modEventBus, ModContainer modContainer) {
		modEventBus.addListener(NeoClientSetup::onClientSetup);

		modContainer.registerExtensionPoint(
			IConfigScreenFactory.class,
			(mc, parent) -> new ConfigScreen(parent)
		);
	}

	private static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() ->
			ClientLogic.getInstance().init(FMLPaths.CONFIGDIR.get())
		);
	}
}
