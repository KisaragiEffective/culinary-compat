package io.github.smokahs.culinarycompat.mixin.client;

import net.blay09.mods.cookingforblockheads.client.gui.screen.KitchenScreen;
import net.blay09.mods.cookingforblockheads.menu.KitchenMenu;
import net.blay09.mods.cookingforblockheads.menu.slot.CraftableListingFakeSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KitchenScreen.class, remap = false)
public abstract class RecipeBook extends AbstractContainerScreen<KitchenMenu> {
	protected RecipeBook(KitchenMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}
	@Inject(method = "render", at = @At("RETURN"))
	private void culinarycompat$shade(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
			CallbackInfo ci) {
		for (var slot : menu.slots) {
			if (slot instanceof CraftableListingFakeSlot listing && listing.getCraftable() != null
					&& listing.getCraftable().isMissingUtensils())
				graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16,
						0xAA1A1A1A);
		}
	}
}
