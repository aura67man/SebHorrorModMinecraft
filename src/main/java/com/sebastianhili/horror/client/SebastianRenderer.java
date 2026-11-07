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
        super.render(entity, yaw, partialTicks, pose, buffers, light);
        pose.popPose();
    }

    @Override
    public Vec3 getRenderOffset(SebastianEntity entity, float partialTicks) {
        return entity.isCrouching() ? new Vec3(0.0D, -0.125D, 0.0D) : super.getRenderOffset(entity, partialTicks);
    }

    /** His nameplate always shows, like a real player's (and through walls, unless he is sneaking). */
    @Override
    protected boolean shouldShowName(SebastianEntity entity) {
        return entity.isAlive() && !entity.isInvisible();
    }

    /** Faintly glowing eyes in the dark; red while he rages. */
    private static final class EyesLayer extends RenderLayer<SebastianEntity, PlayerModel<SebastianEntity>> {
        EyesLayer(RenderLayerParent<SebastianEntity, PlayerModel<SebastianEntity>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, SebastianEntity entity, float limbSwing,
                           float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            boolean rage = entity.getSebState() == SebState.RAGE;
            VertexConsumer vc = buffers.getBuffer(RenderType.eyes(rage ? EYES_RAGE : EYES));
            this.getParentModel().renderToBuffer(pose, vc, 0xF00000, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
