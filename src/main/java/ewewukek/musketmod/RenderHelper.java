package ewewukek.musketmod;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

public class RenderHelper {
    private static int previousSlot = -1;
    public static boolean equipCycleCompleted;

    public static void renderSpecificFirstPersonHand(ItemInHandRenderer renderer, AbstractClientPlayer player, InteractionHand hand, float partialTicks, float interpolatedPitch, float swingProgress, float equipProgress, ItemStack stack, PoseStack matrixStack, MultiBufferSource render, int packedLight) {
        HumanoidArm handside = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        boolean isRightHand = handside == HumanoidArm.RIGHT;
        float sign = isRightHand ? 1 : -1;

        int slot = player.getInventory().selected;
        boolean slotChanged = slot != previousSlot;
        ItemStack clientStack = hand == InteractionHand.MAIN_HAND ? player.getMainHandItem() : player.getOffhandItem();
        if (slotChanged || clientStack.isEmpty() || clientStack.getItem() != MusketMod.MUSKET) equipCycleCompleted = false;

        matrixStack.pushPose();
        matrixStack.translate(sign * 0.15, -0.25, -0.35);

        if (swingProgress > 0) {
            float swingSharp = Mth.sin(Mth.sqrt(swingProgress) * (float)Math.PI);
            float swingNormal = Mth.sin(swingProgress * (float)Math.PI);
            matrixStack.translate(sign * 0.05 * (1 - swingNormal), 0.05 * (1 - swingNormal), 0.05 - 0.4 * swingSharp);
            matrixStack.mulPose(Vector3f.XP.rotationDegrees(180 + sign * 20 * (1 - swingSharp)));

        } else if (player.isUsingItem() && player.getUsedItemHand() == hand) {
            float usingDuration = stack.getUseDuration() - (player.getUseItemRemainingTicks() - partialTicks + 1);
            if (usingDuration > 0 && usingDuration < MusketItem.RELOAD_DURATION) {
                matrixStack.translate(0, -0.3, 0.05);
                matrixStack.mulPose(Vector3f.XP.rotationDegrees(60));
                matrixStack.mulPose(Vector3f.ZP.rotationDegrees(10));

                if (usingDuration >= 8 && usingDuration <= 14 || usingDuration >= 18 && usingDuration <= 24) {
                    if (usingDuration >= 18) usingDuration -= 10;
                    float t;
                    if (usingDuration < 10) {
                        t = (usingDuration - 8) / 2;
                        t = Mth.sin((float)Math.PI / 2 * Mth.sqrt(t));
                    } else {
                        t = (14 - usingDuration) / 4;
                    }
                    matrixStack.translate(0, 0, 0.025 * t);
                }
            }
        } else {
            if (equipCycleCompleted) {
                equipProgress = 0;
            } else {
                // postpone updating previousSlot because slot changing animation
                // sometimes begins with equipProgress == 0
                if (slotChanged) {
                    if (equipProgress > 0.1) previousSlot = slot;
                } else {
                    if (equipProgress == 0) equipCycleCompleted = true;
                }
            }
            matrixStack.translate(0, -0.6 * equipProgress, 0);
        }

        // compensate rotated model
        matrixStack.translate(0, 0.085, 0);
        matrixStack.mulPose(Vector3f.XP.rotationDegrees(-70));

        renderer.renderItem(player, stack, isRightHand ? ItemTransforms.TransformType.FIRST_PERSON_RIGHT_HAND : ItemTransforms.TransformType.FIRST_PERSON_LEFT_HAND, !isRightHand, matrixStack, render, packedLight);

        matrixStack.popPose();
    }
}
