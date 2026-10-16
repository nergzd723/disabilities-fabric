package io.github.nergzd723.disabilities.client.render;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import io.github.nergzd723.disabilities.DisabilitiesMod;
import io.github.nergzd723.disabilities.Disability;
import io.github.nergzd723.disabilities.client.ClientState;

/**
 * Schizophrenia: now and then a dark figure stands at the edge of your vision. It vanishes as soon as you look
 * straight at it (or after 8 seconds).
 */
public final class ShadowFigure {
	private static final Identifier TEXTURE = DisabilitiesMod.id("textures/entity/shadow.png");
	private static final RandomSource RANDOM = RandomSource.create();
	private static final int SPAWN_CHANCE = 180;
	private static final int LIFETIME_TICKS = 160;
	/** Cosine of the angle within which looking at the figure makes it disappear. */
	private static final double NOTICED_DOT = 0.55;
	private static final int TRANSLUCENT_WHITE = 0x80FFFFFF;

	private static @Nullable PlayerModel model;
	private static boolean visible;
	private static double x;
	private static double y;
	private static double z;
	private static int age;

	private ShadowFigure() {
	}

	public static void tick(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.level == null || minecraft.isPaused() || !ClientState.has(Disability.SCHIZOPHRENIA)) {
			visible = false;
			return;
		}
		if (!visible) {
			if (RANDOM.nextInt(SPAWN_CHANCE) == 0) {
				appear(minecraft, player);
			}
			return;
		}
		age++;
		double dx = x - player.getX();
		double dy = y + 0.9 - player.getEyeY();
		double dz = z - player.getZ();
		double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (length > 0.001) {
			Vec3 look = player.getViewVector(1.0F);
			if ((look.x * dx + look.y * dy + look.z * dz) / length > NOTICED_DOT) {
				visible = false;
				return;
			}
		}
		if (age > LIFETIME_TICKS) {
			visible = false;
		}
	}

	/** Somewhere 70-160 degrees to the side of where you're looking, 5-10 blocks away, standing on the ground. */
	private static void appear(Minecraft minecraft, LocalPlayer player) {
		Vec3 look = player.getViewVector(1.0F);
		double side = RANDOM.nextBoolean() ? 1.0 : -1.0;
		double angle = Math.atan2(look.z, look.x) + side * Math.toRadians(70 + RANDOM.nextInt(90));
		double distance = 5 + RANDOM.nextInt(6);
		double figureX = player.getX() + Math.cos(angle) * distance;
		double figureZ = player.getZ() + Math.sin(angle) * distance;
		double figureY = player.getY();
		BlockPos column = BlockPos.containing(figureX, player.getY(), figureZ);
		for (int dy = 4; dy >= -8; dy--) {
			BlockPos feet = column.above(dy);
			if (minecraft.level.getBlockState(feet).isAir() && minecraft.level.getBlockState(feet.above()).isAir()
					&& !minecraft.level.getBlockState(feet.below()).isAir()) {
				figureY = feet.getY();
				break;
			}
		}
		appearAt(figureX, figureY, figureZ);
	}

	/** Makes the figure stand at a specific spot (it still vanishes when looked at). Also used by the game tests. */
	public static void appearAt(double figureX, double figureY, double figureZ) {
		x = figureX;
		y = figureY;
		z = figureZ;
		age = 0;
		visible = true;
	}

	public static void submit(LevelRenderContext context) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!visible || minecraft.player == null) {
			return;
		}
		if (model == null) {
			model = new PlayerModel(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
		}
		Vec3 camera = context.levelState().cameraRenderState.pos;
		// Always face the player.
		float yaw = (float) Math.toDegrees(Math.atan2(minecraft.player.getZ() - z, minecraft.player.getX() - x)) - 90.0F;
		PoseStack poseStack = context.poseStack();
		poseStack.pushPose();
		poseStack.translate(x - camera.x, y - camera.y, z - camera.z);
		poseStack.rotateDegrees(Axis.YP, -yaw);
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		poseStack.translate(0.0F, -1.501F, 0.0F);
		context.submitNodeCollector().submitModel(model, new AvatarRenderState(), poseStack, RenderTypes.entityTranslucent(TEXTURE),
				LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, TRANSLUCENT_WHITE, null, 0);
		poseStack.popPose();
	}

	public static void reset() {
		visible = false;
	}
}
