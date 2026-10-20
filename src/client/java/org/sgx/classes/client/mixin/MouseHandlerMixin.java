package org.sgx.classes.client.mixin;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import org.sgx.classes.item.FoodPouchItem;
import org.sgx.classes.network.CycleFoodPouchPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ctrl + колёсико с мешочком для еды в главной руке переключает выбранную еду (вместо смены слота хотбара). */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void arzClasses$cycleFoodPouch(long window, double xOffset, double yOffset, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() != null || mc.player == null || yOffset == 0.0) {
            return;
        }
        boolean ctrl = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
        if (!ctrl) {
            return;
        }
        ItemStack held = mc.player.getMainHandItem();
        if (!(held.getItem() instanceof FoodPouchItem) || FoodPouchItem.selectedFood(held).isEmpty()) {
            return;
        }
        ClientPlayNetworking.send(new CycleFoodPouchPayload(yOffset > 0 ? -1 : 1));
        ci.cancel();
    }
}
