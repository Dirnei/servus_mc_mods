package io.github.dirnei.mc.servus.delicacies.aquarium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FishPathTest {

    private static final FishPath.Bounds TANK = new FishPath.Bounds(0, 0, 0, 3, 2, 2);

    @Test
    void staysInsideTheBoxMinusTheMargin() {
        FishPath.Bounds inside = TANK.inside(0.25, 2);
        for (long seed = 0; seed < 10; seed++) {
            FishPath path = new FishPath(seed);
            for (int sample = 0; sample < 1000; sample++) {
                FishPath.Pose pose = path.at(sample * 7.3, inside);
                assertTrue(pose.x() >= 0.25 - 1e-9 && pose.x() <= 2.75 + 1e-9, "x " + pose.x());
                assertTrue(pose.y() >= 0.25 - 1e-9 && pose.y() <= 1.75 + 1e-9, "y " + pose.y());
                assertTrue(pose.z() >= 0.25 - 1e-9 && pose.z() <= 1.75 + 1e-9, "z " + pose.z());
            }
        }
    }

    @Test
    void sameSeedSamePath() {
        FishPath a = new FishPath(42);
        FishPath b = new FishPath(42);
        for (int sample = 0; sample < 100; sample++) {
            FishPath.Pose pa = a.at(sample * 3.1, TANK);
            FishPath.Pose pb = b.at(sample * 3.1, TANK);
            assertEquals(pa.x(), pb.x());
            assertEquals(pa.y(), pb.y());
            assertEquals(pa.z(), pb.z());
            assertEquals(pa.yaw(), pb.yaw());
        }
    }

    @Test
    void aLowSurfaceKeepsFishBelowIt() {
        FishPath.Bounds inside = TANK.inside(0.2, 2.0 / 3);
        FishPath path = new FishPath(7);
        for (int sample = 0; sample < 10_000; sample++) {
            assertTrue(path.at(sample, inside).y() <= 2.0 / 3 - 0.2 + 1e-9);
        }
    }

    @Test
    void headingFollowsTheMotion() {
        FishPath path = new FishPath(3);
        double time = 100;
        FishPath.Pose now = path.at(time, TANK);
        FishPath.Pose next = path.at(time + 0.01, TANK);
        double yaw = Math.atan2(next.x() - now.x(), next.z() - now.z());
        assertEquals(yaw, now.yaw(), 1e-2);
    }
}
