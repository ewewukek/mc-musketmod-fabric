package ewewukek.musketmod;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3f;

@Environment(EnvType.CLIENT)
public class RenderHelper {
    private static int previousSlot = -1;
    public static boolean equipCycleCompleted;

    public static void renderSpecificFirstPersonHand(HeldItemRenderer renderer, AbstractClientPlayerEntity player, Hand hand, float partialTicks, float interpolatedPitch, float swingProgress, float equipProgress, ItemStack stack, MatrixStack matrixStack, VertexConsumerProvider render, int packedLight) {
        Arm handside = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        boolean isRightHand = handside == Arm.RIGHT;
        float sign = isRightHand ? 1 : -1;

        int slot = player.getInventory().selectedSlot;
        boolean slotChanged = slot != previousSlot;
        ItemStack clientStack = hand == Hand.MAIN_HAND ? player.getMainHandStack() : player.getOffHandStack();
        if (slotChanged || clientStack.isEmpty() || clientStack.getItem() != MusketMod.MUSKET) equipCycleCompleted = false;

        matrixStack.push();

        if (swingProgress > 0) {
            float swingSharp = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float)Math.PI);
            float swingNormal = MathHelper.sin(swingProgress * (float)Math.PI);
            matrixStack.translate(sign * (0.2f - 0.05f * swingNormal), -0.2f - 0.05f * swingNormal, -0.3f - 0.4f * swingSharp);
            matrixStack.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(180 + sign * (20 - 20 * swingSharp)));

        } else {
            float usingDuration = stack.getMaxUseTime() - (player.getItemUseTimeLeft() - partialTicks + 1);
            boolean isLoading = player.isUsingItem() && player.getActiveHand() == hand && !MusketItem.isLoaded(stack)
                                && usingDuration > 0 && usingDuration < MusketItem.RELOAD_DURATION;
            if (isLoading) {
                matrixStack.translate(sign * 0.15f, -0.55f, -0.3f);
                matrixStack.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(60));
                matrixStack.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(10));

                if (usingDuration >= 8 && usingDuration <= 14 || usingDuration >= 18 && usingDuration <= 24) {
                    if (usingDuration >= 18) usingDuration -= 10;
                    float t;
                    if (usingDuration < 10) {
                        t = (usingDuration - 8) / 2;
                        t = MathHelper.sin((float)Math.PI / 2 * MathHelper.sqrt(t));
                    } else {
                        t = (14 - usingDuration) / 4;
                    }
                    matrixStack.translate(0, 0, 0.02f * t);
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
                        if (equipProgress == 0f) equipCycleCompleted = true;
                    }
                }
                matrixStack.translate(sign * 0.15f, -0.27f + equipProgress * -0.6f, -0.37f);
            }
        }

        // compensate rotated model
        matrixStack.translate(0, 0.085f, 0);
        matrixStack.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-70));

        renderer.renderItem(player, stack, isRightHand ? ModelTransformation.Mode.FIRST_PERSON_RIGHT_HAND : ModelTransformation.Mode.FIRST_PERSON_LEFT_HAND, !isRightHand, matrixStack, render, packedLight);

        matrixStack.pop();
    }
}
