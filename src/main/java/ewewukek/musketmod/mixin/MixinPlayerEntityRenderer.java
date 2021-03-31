package ewewukek.musketmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ewewukek.musketmod.MusketItem;
import ewewukek.musketmod.MusketMod;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

@Mixin(PlayerEntityRenderer.class)
public class MixinPlayerEntityRenderer {
    @Inject(
        method = "getArmPose",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void getArmPose(AbstractClientPlayerEntity player, Hand hand, CallbackInfoReturnable<BipedEntityModel.ArmPose> ci) {
        if (!player.handSwinging) {
            ItemStack stack = player.getStackInHand(hand);
            if (!stack.isEmpty() && stack.getItem() == MusketMod.MUSKET && MusketItem.isLoaded(stack)) {
                ci.setReturnValue(BipedEntityModel.ArmPose.CROSSBOW_HOLD);
                ci.cancel();
            }
        }
    }
}
