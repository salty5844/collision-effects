package salty5844.collisioneffects.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.resources.Identifier;
import salty5844.collisioneffects.client.core.ClientLogic;

@Mod.EventBusSubscriber(modid = "collision_effects", value = Dist.CLIENT)
public final class ForgeGameEvents {

	@SubscribeEvent
	public static void onAddGuiOverlayLayers(AddGuiOverlayLayersEvent event) {
		event.getLayeredDraw().add(
			ForgeLayeredDraw.VANILLA_ROOT,
			Identifier.fromNamespaceAndPath("collision_effects", "hud"),
			(graphics, deltaTracker) -> ClientLogic.getInstance().renderHud(graphics)
		);
	}

	@SubscribeEvent
	public static void onAttackEntity(AttackEntityEvent event) {
		if (event.getEntity() == Minecraft.getInstance().player) {
			ClientLogic.getInstance().onEntityAttacked(event.getTarget());
		}
	}

	@SubscribeEvent
	public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
		ClientLogic.getInstance().onWorldUnload();
	}
}