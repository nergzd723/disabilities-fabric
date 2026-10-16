package io.github.nergzd723.disabilities.client.render;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;

/** Prosopagnosia: every mob is drawn as a plain Steve that copies the mob's pose and animation. */
public final class MobDisguise {
	private static final Identifier STEVE = Identifier.withDefaultNamespace("textures/entity/player/wide/steve.png");
	private static @Nullable PlayerModel model;

	private MobDisguise() {
	}

	public static void submit(LivingEntityRenderState mob, PoseStack poseStack, SubmitNodeCollector collector) {
		if (mob.isInvisible) {
			return;
		}
		if (model == null) {
			model = new PlayerModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
		}
		poseStack.pushPose();
		poseStack.scale(mob.scale, mob.scale, mob.scale);
		poseStack.rotateDegrees(Axis.YP, 180.0F - mob.bodyRot);
		if (mob.deathTime > 0.0F) {
			// Fall over like any dying mob would.
			float fall = Math.min(1.0F, Mth.sqrt((mob.deathTime - 1.0F) / 20.0F * 1.6F));
			poseStack.rotateDegrees(Axis.ZP, fall * 90.0F);
		}
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		poseStack.translate(0.0F, -1.501F, 0.0F);
		collector.submitModel(model, steveFor(mob), poseStack, RenderTypes.entityCutout(STEVE), mob.lightCoords,
				LivingEntityRenderer.getOverlayCoords(mob, 0.0F), -1, null, mob.outlineColor);
		poseStack.popPose();
	}

	private static AvatarRenderState steveFor(LivingEntityRenderState mob) {
		AvatarRenderState steve = new AvatarRenderState();
		steve.yRot = mob.yRot;
		steve.bodyRot = mob.bodyRot;
		steve.xRot = mob.xRot;
		steve.walkAnimationPos = mob.walkAnimationPos;
		steve.walkAnimationSpeed = mob.walkAnimationSpeed;
		steve.ageInTicks = mob.ageInTicks;
		steve.lightCoords = mob.lightCoords;
		if (mob instanceof ArmedEntityRenderState armed) {
			steve.currentSwing = armed.currentSwing;
			steve.swingAnimation = armed.swingAnimation;
			steve.mainArm = armed.mainArm;
		}
		boolean crouching = mob.isDiscrete;
		if (mob instanceof HumanoidRenderState humanoid) {
			steve.swimAmount = humanoid.swimAmount;
			steve.speedValue = Math.max(1.0F, humanoid.speedValue);
			crouching = humanoid.isCrouching;
		}
		steve.isDiscrete = crouching;
		steve.isCrouching = crouching;
		steve.pose = crouching ? Pose.CROUCHING : Pose.STANDING;
		return steve;
	}
}
