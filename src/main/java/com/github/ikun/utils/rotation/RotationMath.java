// SPDX-License-Identifier: GPL-3.0-only
// Math extracted without numerical changes from RatAddons 2.0.1 HumanRotation.
package com.github.ikun.utils.rotation;

import net.minecraft.util.Mth;

public final class RotationMath {
    private RotationMath() {}

    public record Angles(float yaw, float pitch) {}

    public static Angles quantize(float currentYaw, float currentPitch, float yaw, float pitch, float sensitivity) {
        float multiplier = sensitivity * 0.6f + 0.2f;
        float gcd = multiplier * multiplier * multiplier * 1.2f;
        float yawStep = (float)Math.round(Mth.wrapDegrees(yaw - currentYaw) / gcd) * gcd;
        float pitchStep = (float)Math.round((pitch - currentPitch) / gcd) * gcd;
        return new Angles(currentYaw + yawStep, Math.clamp(currentPitch + pitchStep, -90.0f, 90.0f));
    }

    public static float smootherStep(float p) {
        return p * p * p * (p * (p * 6.0f - 15.0f) + 10.0f);
    }

    public static float lerp(float a, float b, float p) {
        return a + (b - a) * p;
    }

    public static float pulse(float progress, float center, float width) {
        float distance = Math.abs(progress - center);
        if (distance >= width) return 0.0f;
        return (float)Math.sin((double)(1.0f - distance / width) * 1.5707963267948966);
    }
}
