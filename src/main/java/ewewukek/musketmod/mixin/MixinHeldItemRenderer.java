package ewewukek.musketmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ewewukek.musketmod.MusketMod;
import ewewukek.musketmod.RenderHelper;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

@Mixin(HeldItemRenderer.class)
public class MixinHeldItemRenderer {
    @Inject(
        method = "renderFirstPersonItem",
        at = @At(value ="HEAD"),
        cancellable = true
    )
    private void renderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (stack.getItem() == MusketMod.MUSKET) {
            RenderHelper.renderSpecificFirstPersonHand((HeldItemRenderer)(Object)this, player, hand, tickDelta, pitch, swingProgress, equipProgress, stack, matrices,vertexConsumers, light);
            ci.cancel();
        }
    }
}
