package com.sebastianhili.horror.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sebastianhili.horror.SebHorrorMod;
import com.sebastianhili.horror.entity.SebState;
import com.sebastianhili.horror.entity.SebastianEntity;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Renders Sebastian with the full player model (classic arms, all overlay layers) and his skin. */
public class SebastianRenderer extends HumanoidMobRenderer<SebastianEntity, PlayerModel<SebastianEntity>> {
    public static final ResourceLocation SKIN = ResourceLocation.fromNamespaceAndPath(SebHorrorMod.MODID, "textures/entity/sebastian_hili.png");
    private static final ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath(SebHorrorMod.MODID, "textures/entity/sebastian_hili_eyes.png");
    private static final ResourceLocation EYES_RAGE = ResourceLocation.fromNamespaceAndPath(SebHorrorMod.MODID, "textures/entity/sebastian_hili_eyes_rage.png");

    public SebastianRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidArmorModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                ctx.getModelManager()));
        this.addLayer(new EyesLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(SebastianEntity entity) {
        return SKIN;
    }

    @Override
    public void render(SebastianEntity entity, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        this.model.crouching = entity.isCrouching();
        pose.pushPose();
        if (entity.getSebState() == SebState.RAGE) {
            // shaking with fury
            pose.translate((entity.getRandom().nextFloat() - 0.5F) * 0.08F, 0, (entity.getRandom().nextFloat() - 0.5F) * 0.08F);
        }
        float fade = entity.getFade();
        if (fade <= 0.01F) {
            pose.popPose();
            return;
        }
        // in The Red he fades out at the end of the song by drawing his skin see-through
        MultiBufferSource out = fade < 0.999F ? new FadeBuffers(buffers, fade) : buffers;
        super.render(entity, yaw, partialTicks, pose, out, light);
        pose.popPose();
    }

    /** The boss is a little bigger. */
    @Override
    protected void scale(SebastianEntity entity, PoseStack pose, float partialTicks) {
        if (entity.isBoss()) pose.scale(1.2F, 1.2F, 1.2F);
    }

    @Override
    public Vec3 getRenderOffset(SebastianEntity entity, float partialTicks) {
        return entity.isCrouching() ? new Vec3(0.0D, -0.125D, 0.0D) : super.getRenderOffset(entity, partialTicks);
    }

    /** His nameplate always shows, like a real player's (and through walls, unless he is sneaking). */
    @Override
    protected boolean shouldShowName(SebastianEntity entity) {
        return entity.isAlive() && !entity.isInvisible() && !entity.isBoss();
    }

    /** Swaps his opaque skin layer for a translucent one and scales every vertex's alpha. */
    private static final class FadeBuffers implements MultiBufferSource {
        private final MultiBufferSource inner;
        private final float alpha;

        FadeBuffers(MultiBufferSource inner, float alpha) {
            this.inner = inner;
            this.alpha = alpha;
        }

        @Override
        public VertexConsumer getBuffer(RenderType type) {
            if (type == RenderType.entityCutoutNoCull(SKIN)) {
                return new AlphaConsumer(inner.getBuffer(RenderType.entityTranslucent(SKIN)), alpha, false);
            }
            return new AlphaConsumer(inner.getBuffer(type), alpha, true); // eyes/items: dim them too
        }
    }

    private record AlphaConsumer(VertexConsumer d, float alpha, boolean dimColour) implements VertexConsumer {
        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            d.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            d.color(r, g, b, (int) (a * alpha));
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            d.uv(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            d.overlayCoords(u, v);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            d.uv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            d.normal(x, y, z);
            return this;
        }

        @Override
        public void endVertex() {
            d.endVertex();
        }

        @Override
        public void vertex(float x, float y, float z, float r, float g, float b, float a, float u, float v,
                           int overlay, int light, float nx, float ny, float nz) {
            float k = dimColour ? alpha : 1.0F;
            d.vertex(x, y, z, r * k, g * k, b * k, a * alpha, u, v, overlay, light, nx, ny, nz);
        }

        @Override
        public void defaultColor(int r, int g, int b, int a) {
            d.defaultColor(r, g, b, (int) (a * alpha));
        }

        @Override
        public void unsetDefaultColor() {
            d.unsetDefaultColor();
        }
    }

    /** Faintly glowing eyes in the dark; red while he rages. */
    private static final class EyesLayer extends RenderLayer<SebastianEntity, PlayerModel<SebastianEntity>> {
        EyesLayer(RenderLayerParent<SebastianEntity, PlayerModel<SebastianEntity>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, SebastianEntity entity, float limbSwing,
                           float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            boolean rage = entity.getSebState() == SebState.RAGE || entity.isBoss();
            VertexConsumer vc = buffers.getBuffer(RenderType.eyes(rage ? EYES_RAGE : EYES));
            this.getParentModel().renderToBuffer(pose, vc, 0xF00000, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
