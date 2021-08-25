package ewewukek.musketmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ewewukek.musketmod.GunItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

@Mixin(PlayerRenderer.class)
public class MixinPlayerEntityRenderer {
    @Inject(
        method = "getArmPose",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void getArmPose(AbstractClientPlayer player, InteractionHand hand, CallbackInfoReturnable<HumanoidModel.ArmPose> ci) {
        if (!player.swinging && hand == InteractionHand.MAIN_HAND) {
            ItemStack stack = player.getMainHandItem();
            if (!stack.isEmpty() && stack.getItem() instanceof GunItem && GunItem.isLoaded(stack)) {
                ci.setReturnValue(HumanoidModel.ArmPose.CROSSBOW_HOLD);
                ci.cancel();
            }
        }
    }
}
