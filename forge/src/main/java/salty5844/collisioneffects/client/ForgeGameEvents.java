package salty5844.collisioneffects.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import salty5844.collisioneffects.client.core.ClientLogic;

@Mod.EventBusSubscriber(modid = "collision_effects", value = Dist.CLIENT)
public final class ForgeGameEvents {

	@SubscribeEvent
	public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
		ClientLogic.getInstance().renderHud(event.getGuiGraphics());
	}

	@SubscribeEvent
	public static void onAttackEntity(AttackEntityEvent event) {
		if (event.getEntity() == Minecraft.getInstance().player) {
			ClientLogic.getInstance().onEntityAttacked(event.getTarget());
		}
	}
}