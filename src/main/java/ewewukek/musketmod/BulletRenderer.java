package ewewukek.musketmod;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;

@Environment(EnvType.CLIENT)
public class BulletRenderer extends EntityRenderer<BulletEntity> {
    public static final Identifier TEXTURE = new Identifier(MusketMod.MODID + ":textures/entity/bullet.png");

    protected BulletRenderer(EntityRenderDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public Identifier getTexture(BulletEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(BulletEntity bullet, float yaw, float partialTicks, MatrixStack matrixStack, VertexConsumerProvider render, int packedLight) {
        if (bullet.isFirstTick()) return;

        matrixStack.push();

        matrixStack.scale(0.1f, 0.1f, 0.1f);
        // billboarding
        matrixStack.multiply(dispatcher.getRotation());
        matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(180));

        MatrixStack.Entry entry = matrixStack.peek();
        Matrix4f positionMatrix = entry.getModel();
        Matrix3f normalMatrix = entry.getNormal();

        VertexConsumer builder = render.getBuffer(RenderLayer.getEntityCutout(getTexture(bullet)));

        addVertex(builder, positionMatrix, normalMatrix, -1, -1, 0, 0, 1, 0, 0, 1, packedLight);
        addVertex(builder, positionMatrix, normalMatrix,  1, -1, 0, 1, 1, 0, 0, 1, packedLight);
        addVertex(builder, positionMatrix, normalMatrix,  1,  1, 0, 1, 0, 0, 0, 1, packedLight);
        addVertex(builder, positionMatrix, normalMatrix, -1,  1, 0, 0, 0, 0, 0, 1, packedLight);

        matrixStack.pop();
    }

    void addVertex(VertexConsumer builder, Matrix4f positionMatrix, Matrix3f normalMatrix, float x, float y, float z, float u, float v, float nx, float ny, float nz, int packedLight) {
        builder.vertex(positionMatrix, x, y, z)
               .color(255, 255, 255, 255)
               .texture(u, v)
               .overlay(OverlayTexture.DEFAULT_UV)
               .light(packedLight)
               .normal(normalMatrix, nx, ny, nz)
               .next();
    }
}
