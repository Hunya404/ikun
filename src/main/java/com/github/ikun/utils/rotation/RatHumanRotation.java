// SPDX-License-Identifier: GPL-3.0-only
// Adapted from RatTokens' RatAddons 2.0.1 HumanRotation.
// Preserves the original rotation calculations with movement/click actions omitted.
package com.github.ikun.utils.rotation;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class RatHumanRotation {
    private static final float TWO_PI = (float)Math.PI * 2;
    private static final double NANOS_PER_SECOND = 1.0E9;
    private static Rotation current;
    private static long lastFrameNanos;

    private RatHumanRotation() {
    }

    public static boolean isRotating() {
        return current != null;
    }

    public static float[] look(Vec3 target, Vec3 origin) {
        double dx = target.x - origin.x;
        double dy = target.y - origin.y;
        double dz = target.z - origin.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        return new float[]{(float)Math.toDegrees(Math.atan2(-dx, dz)), (float)Math.toDegrees(-Math.atan2(dy, horizontal))};
    }

    public static void lookAt(Vec3 target, float pitch, float yawOffset, float pitchOffset, Profile profile) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        float yaw = RatHumanRotation.look(target, player.getEyePosition(RatHumanRotation.partialTick()))[0];
        RatHumanRotation.start(yaw + yawOffset, pitch + pitchOffset, target, true, false, yawOffset, pitchOffset, profile);
    }

    public static void cancel() {

        current = null;
        lastFrameNanos = 0L;
    }

    private static void start(float yaw, float pitch, Vec3 worldTarget, boolean trackYaw, boolean trackPitch, float targetYawOffset, float targetPitchOffset, Profile profile) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        current = null;
        float startYaw = player.getYRot();
        float startPitch = player.getXRot();
        float goalYaw = startYaw + Mth.wrapDegrees((float)(yaw - startYaw));
        float goalPitch = Math.clamp(pitch, -90.0f, 90.0f);
        float dy = goalYaw - startYaw;
        float dp = goalPitch - startPitch;
        float distance = (float)Math.hypot(dy, dp);
        if (distance < 0.01f) {
            RatHumanRotation.applyGcd(goalYaw, goalPitch);
            lastFrameNanos = 0L;
            return;
        }
        float duration = Math.clamp(distance / RatHumanRotation.random(profile.minSpeed(), profile.maxSpeed()), profile.minDuration(), profile.maxDuration());
        float overshoot = distance > 3.0f && RatHumanRotation.nextFloat() < profile.overshootChance() ? RatHumanRotation.random(profile.minOvershoot(), profile.maxOvershoot()) * Math.clamp(distance / 45.0f, 0.25f, 1.0f) : 0.0f;
        float unitYaw = dy / distance;
        float unitPitch = dp / distance;
        float correctionTime = overshoot > 0.0f ? Math.max(0.065f, duration * RatHumanRotation.random(0.12f, 0.22f)) : 0.0f;
        float mainTime = Math.max(0.035f, duration - correctionTime);
        boolean hasFlick = distance > 8.0f && profile.maxFlick() > 0.0f && RatHumanRotation.nextFloat() < profile.flickChance();
        float flick = hasFlick ? RatHumanRotation.random(profile.maxFlick() * 0.25f, profile.maxFlick()) : 0.0f;
        float side = ThreadLocalRandom.current().nextBoolean() ? 1.0f : -1.0f;
        long now = System.nanoTime();
        Rotation rotation = new Rotation();
        rotation.startYaw = startYaw;
        rotation.startPitch = startPitch;
        rotation.goalYaw = goalYaw;
        rotation.goalPitch = goalPitch;
        rotation.overYawOffset = unitYaw * overshoot;
        rotation.overPitchOffset = Math.clamp(goalPitch + unitPitch * overshoot, -90.0f, 90.0f) - goalPitch;
        rotation.mainTime = mainTime;
        rotation.correctionTime = correctionTime;
        rotation.flickCenter = RatHumanRotation.random(0.34f, 0.72f);
        rotation.flickWidth = RatHumanRotation.random(0.045f, 0.09f);
        rotation.flickYaw = -unitPitch * flick * side;
        rotation.flickPitch = unitYaw * flick * side;
        rotation.noise = profile.microCorrection() * Math.clamp(distance / 30.0f, 0.25f, 1.0f);
        rotation.yawPhase = RatHumanRotation.random(0.0f, (float)Math.PI * 2);
        rotation.pitchPhase = RatHumanRotation.random(0.0f, (float)Math.PI * 2);
        rotation.yawFrequency = RatHumanRotation.random(1.7f, 2.8f);
        rotation.pitchFrequency = RatHumanRotation.random(2.0f, 3.2f);
        rotation.worldTarget = worldTarget;
        rotation.trackTargetYaw = trackYaw;
        rotation.trackTargetPitch = trackPitch;
        rotation.targetYawOffset = targetYawOffset;
        rotation.targetPitchOffset = targetPitchOffset;
        rotation.startedAtNanos = now;
        rotation.timeoutNanos = (long)((double)profile.timeout() * 1.0E9);
        current = rotation;
        lastFrameNanos = now;
    }

    public static void onFrame() {
        float basePitch;
        float baseYaw;
        float p;
        Minecraft client = Minecraft.getInstance();
        if (client.screen != null) {
            return;
        }
        Rotation state = current;
        if (state == null) {
            return;
        }
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            RatHumanRotation.cancel();
            return;
        }
        long now = System.nanoTime();
        if (now - state.startedAtNanos >= state.timeoutNanos) {

            current = null;
            lastFrameNanos = 0L;
            return;
        }
        if (lastFrameNanos == 0L) {
            lastFrameNanos = now;
            return;
        }
        float frameTime = Math.clamp((float)((double)(now - lastFrameNanos) / 1.0E9), 0.0f, 0.05f);
        lastFrameNanos = now;
        state.elapsed += frameTime;
        if (state.worldTarget != null) {
            float[] look = RatHumanRotation.look(state.worldTarget, player.getEyePosition(RatHumanRotation.partialTick()));
            if (state.trackTargetYaw) {
                state.goalYaw += Mth.wrapDegrees((float)(look[0] + state.targetYawOffset - state.goalYaw));
            }
            if (state.trackTargetPitch) {
                state.goalPitch = Math.clamp(look[1] + state.targetPitchOffset, -90.0f, 90.0f);
            }
        }
        if (state.elapsed >= state.duration()) {
            RatHumanRotation.applyGcd(state.goalYaw, state.goalPitch);

            current = null;
            lastFrameNanos = 0L;
            return;
        }
        float totalProgress = Math.clamp(state.elapsed / state.duration(), 0.0f, 1.0f);
        float overYaw = state.goalYaw + state.overYawOffset;
        float overPitch = Math.clamp(state.goalPitch + state.overPitchOffset, -90.0f, 90.0f);
        if (state.elapsed < state.mainTime || state.correctionTime == 0.0f) {
            p = RotationMath.smootherStep(Math.clamp(state.elapsed / state.mainTime, 0.0f, 1.0f));
            baseYaw = RotationMath.lerp(state.startYaw, overYaw, p);
            basePitch = RotationMath.lerp(state.startPitch, overPitch, p);
        } else {
            p = RotationMath.smootherStep(Math.clamp((state.elapsed - state.mainTime) / state.correctionTime, 0.0f, 1.0f));
            baseYaw = RotationMath.lerp(overYaw, state.goalYaw, p);
            basePitch = RotationMath.lerp(overPitch, state.goalPitch, p);
        }
        float flick = RotationMath.pulse(totalProgress, state.flickCenter, state.flickWidth);
        float envelope = (float)Math.sin(Math.PI * (double)totalProgress);
        float yawNoise = (float)Math.sin(state.yawPhase + totalProgress * ((float)Math.PI * 2) * state.yawFrequency) * state.noise * envelope;
        float pitchNoise = (float)Math.sin(state.pitchPhase + totalProgress * ((float)Math.PI * 2) * state.pitchFrequency) * state.noise * 0.65f * envelope;
        RatHumanRotation.applyGcd(baseYaw + state.flickYaw * flick + yawNoise, basePitch + state.flickPitch * flick + pitchNoise);
    }

    public static void applyGcd(float yaw, float pitch) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }
        RotationMath.Angles angles = RotationMath.quantize(player.getYRot(), player.getXRot(), yaw, pitch,
                ((Double)client.options.sensitivity().get()).floatValue());
        player.setYRot(angles.yaw());
        player.setXRot(angles.pitch());
    }

    public static float partialTick() {
        return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
    }

    private static float nextFloat() {
        return ThreadLocalRandom.current().nextFloat();
    }

    private static float random(float min, float max) {
        return min == max ? min : RatHumanRotation.nextFloat() * (max - min) + min;
    }

    private static final class Rotation {
        float startYaw;
        float startPitch;
        float goalYaw;
        float goalPitch;
        float overYawOffset;
        float overPitchOffset;
        float mainTime;
        float correctionTime;
        float flickCenter;
        float flickWidth;
        float flickYaw;
        float flickPitch;
        float noise;
        float yawPhase;
        float pitchPhase;
        float yawFrequency;
        float pitchFrequency;
        Vec3 worldTarget;
        boolean trackTargetYaw;
        boolean trackTargetPitch;
        float targetYawOffset;
        float targetPitchOffset;
        long startedAtNanos;
        long timeoutNanos;
        float elapsed;

        private Rotation() {
        }

        float duration() {
            return this.mainTime + this.correctionTime;
        }
    }

    public record Profile(float minSpeed, float maxSpeed, float minDuration, float maxDuration, float overshootChance, float minOvershoot, float maxOvershoot, float flickChance, float maxFlick, float microCorrection, float timeout) {
        public static Profile speeds(float minSpeed, float maxSpeed) {
            return new Profile(minSpeed, Math.max(minSpeed, maxSpeed), 0.1f, 0.9f, 0.78f, 0.2f, 1.65f, 0.0f, 0.05f, 0.075f, 1.5f);
        }
    }

}

