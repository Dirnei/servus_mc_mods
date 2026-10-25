package io.github.dirnei.mc.servus.shuttles.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ArmPoseTest {

    private static final double REST = 0.25;
    private static final double MAX = 3;
    private static final double DROP = 0.6;

    @Test
    void foldedAtRest() {
        ArmPose.Pose pose = ArmPose.of(3, DROP, 90, 0, 0, REST, MAX);
        assertEquals(0, pose.yaw(), 1e-9);
        assertEquals(0, pose.lower(), 1e-9);
        assertEquals(180, pose.upper(), 1e-9);
        assertEquals(REST, pose.length(), 1e-9);
        assertEquals(0, pose.headX(), 1e-9);
        assertEquals(1, pose.clawOpening(), 1e-9);
    }

    @Test
    void headReachesTheTrunkForDistancesOneToFour() {
        for (int distance = 1; distance <= 4; distance++) {
            ArmPose.Pose pose = ArmPose.of(distance, DROP, -90, 1, 1, REST, MAX);
            assertEquals(distance, pose.headX(), 1e-9, "distance " + distance);
            assertEquals(-DROP, pose.headY(), 1e-9, "distance " + distance);
            assertEquals(-90, pose.yaw(), 1e-9);
            assertEquals(0, pose.clawOpening(), 1e-9);
            assertTrue(pose.lower() > pose.upper(), "the elbow stays up");
        }
    }

    @Test
    void unreachableDistanceClamps() {
        ArmPose.Pose pose = ArmPose.of(20, DROP, 0, 1, 0, REST, MAX);
        assertEquals(MAX, pose.length(), 1e-9);
        assertEquals(2 * MAX, Math.hypot(pose.headX(), pose.headY()), 1e-9);
        assertTrue(pose.headX() > 0 && pose.headY() < 0, "it still reaches toward the trunk");
    }
}
