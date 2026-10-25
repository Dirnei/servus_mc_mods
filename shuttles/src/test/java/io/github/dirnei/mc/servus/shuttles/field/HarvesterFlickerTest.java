package io.github.dirnei.mc.servus.shuttles.field;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.shuttles.field.HarvesterParts.Box;
import io.github.dirnei.mc.servus.shuttles.field.HarvesterPose.Placed;
import io.github.dirnei.mc.servus.shuttles.field.HarvesterTimeline.Moment;
import io.github.dirnei.mc.servus.shuttles.field.HarvesterTimeline.Stage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * No two faces of the Lumber hold, crane and head share a plane where they
 * overlap, at rest, folded, unfolding, reaching, gripping, cutting and backed
 * off, for every tier and for trees on both sides: such faces flicker.
 */
class HarvesterFlickerTest {

    private static final double FLOOR = -2.5;
    private static final double EPS = 1e-4;

    private record Face(String owner, double[][] corners, double[] normal, double offset) {
    }

    @Test
    void closedHoldHasNoSharedPlanes() {
        for (int tier = 1; tier <= 3; tier++) {
            List<Face> faces = new ArrayList<>();
            List<Box> hold = HarvesterParts.hold(tier);
            for (int i = 0; i < hold.size(); i++) {
                faces.addAll(faces("hold box " + i + " " + hold.get(i), Frame.IDENTITY, hold.get(i)));
            }
            assertNoSharedPlanes("Mk" + tier + " closed hold", faces);
        }
    }

    @Test
    void posedPartsHaveNoSharedPlanes() {
        List<Moment> moments = new ArrayList<>();
        for (Stage stage : Stage.values()) {
            for (double p : new double[] {0, 0.37, 1}) {
                moments.add(new Moment(stage, p));
            }
        }
        double b = HarvesterPose.BLOCK;
        List<double[]> logs = List.of(new double[] {8 + b, FLOOR, 8}, new double[] {8 + 2 * b, FLOOR, 8}, new double[] {8 - 3 * b, FLOOR, 8},
                new double[] {8 + 4 * b, FLOOR, 8 + b}, new double[] {8, FLOOR, 8 + 2 * b});
        for (int tier = 1; tier <= 3; tier++) {
            for (double[] log : logs) {
                for (Moment moment : moments) {
                    List<Face> faces = new ArrayList<>();
                    int n = 0;
                    for (Placed placed : HarvesterPose.of(log, moment).parts()) {
                        n++;
                        for (Box box : HarvesterParts.boxes(placed.part(), tier)) {
                            faces.addAll(faces(placed.part() + "#" + n + " " + box, placed.frame(), box));
                        }
                    }
                    assertNoSharedPlanes("Mk" + tier + " log " + log[0] + "," + log[2] + " " + moment, faces);
                }
            }
        }
    }

    private static void assertNoSharedPlanes(String where, List<Face> faces) {
        List<String> clashes = new ArrayList<>();
        for (int i = 0; i < faces.size(); i++) {
            for (int j = i + 1; j < faces.size(); j++) {
                Face a = faces.get(i);
                Face c = faces.get(j);
                if (a.owner.equals(c.owner) || dot(a.normal, c.normal) < 1 - 1e-6 || Math.abs(a.offset - c.offset) > EPS) {
                    continue;
                }
                double area = overlap(a, c);
                if (area > EPS) {
                    clashes.add(a.owner + " / " + c.owner + " area " + area);
                }
            }
        }
        assertTrue(clashes.isEmpty(), where + ": " + clashes.size() + " shared planes\n" + String.join("\n", clashes.subList(0, Math.min(12,
                clashes.size()))));
    }

    private static final int[][] QUADS = {{0, 4, 6, 2}, {1, 3, 7, 5}, {0, 1, 5, 4}, {2, 6, 7, 3}, {0, 2, 3, 1}, {4, 5, 7, 6}};

    private static List<Face> faces(String owner, Frame frame, Box box) {
        double[][] corners = HarvesterPoseTest.corners(box);
        double[][] p = new double[8][];
        for (int i = 0; i < 8; i++) {
            p[i] = frame.apply(corners[i]);
        }
        double[] centre = mid(p[0], p[7]);
        List<Face> faces = new ArrayList<>();
        for (int[] quad : QUADS) {
            double[][] q = {p[quad[0]], p[quad[1]], p[quad[2]], p[quad[3]]};
            double[] n = cross(sub(q[1], q[0]), sub(q[3], q[0]));
            double len = Math.sqrt(dot(n, n));
            if (len < 1e-12) {
                continue;
            }
            n = new double[] {n[0] / len, n[1] / len, n[2] / len};
            if (dot(n, sub(mid(q[0], q[2]), centre)) < 0) {
                n = new double[] {-n[0], -n[1], -n[2]};
            }
            faces.add(new Face(owner, q, n, dot(n, q[0])));
        }
        return faces;
    }

    /** The area two coplanar quads share. */
    private static double overlap(Face a, Face c) {
        double[] u = sub(a.corners[1], a.corners[0]);
        double ul = Math.sqrt(dot(u, u));
        u = new double[] {u[0] / ul, u[1] / ul, u[2] / ul};
        double[] v = cross(a.normal, u);
        List<double[]> pa = flat(a.corners, u, v);
        List<double[]> pc = flat(c.corners, u, v);
        return area(clip(pa, pc));
    }

    private static List<double[]> flat(double[][] corners, double[] u, double[] v) {
        List<double[]> points = new ArrayList<>();
        for (double[] corner : corners) {
            points.add(new double[] {dot(corner, u), dot(corner, v)});
        }
        if (signedArea(points) < 0) {
            points = new ArrayList<>(points.reversed());
        }
        return points;
    }

    /** Sutherland-Hodgman: the subject polygon clipped to the convex clip polygon, both counter-clockwise. */
    private static List<double[]> clip(List<double[]> subject, List<double[]> clip) {
        List<double[]> out = subject;
        for (int i = 0; i < clip.size() && !out.isEmpty(); i++) {
            double[] e0 = clip.get(i);
            double[] e1 = clip.get((i + 1) % clip.size());
            List<double[]> in = out;
            out = new ArrayList<>();
            for (int k = 0; k < in.size(); k++) {
                double[] s = in.get(k);
                double[] e = in.get((k + 1) % in.size());
                boolean sIn = side(e0, e1, s) >= -1e-12;
                boolean eIn = side(e0, e1, e) >= -1e-12;
                if (eIn) {
                    if (!sIn) {
                        out.add(intersect(e0, e1, s, e));
                    }
                    out.add(e);
                } else if (sIn) {
                    out.add(intersect(e0, e1, s, e));
                }
            }
        }
        return out;
    }

    private static double side(double[] a, double[] b, double[] p) {
        return (b[0] - a[0]) * (p[1] - a[1]) - (b[1] - a[1]) * (p[0] - a[0]);
    }

    private static double[] intersect(double[] a, double[] b, double[] s, double[] e) {
        double d1 = side(a, b, s);
        double d2 = side(a, b, e);
        double t = d1 / (d1 - d2);
        return new double[] {s[0] + (e[0] - s[0]) * t, s[1] + (e[1] - s[1]) * t};
    }

    private static double area(List<double[]> polygon) {
        return polygon.size() < 3 ? 0 : Math.abs(signedArea(polygon));
    }

    private static double signedArea(List<double[]> polygon) {
        double sum = 0;
        for (int i = 0; i < polygon.size(); i++) {
            double[] a = polygon.get(i);
            double[] b = polygon.get((i + 1) % polygon.size());
            sum += a[0] * b[1] - b[0] * a[1];
        }
        return sum / 2;
    }

    private static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static double[] mid(double[] a, double[] b) {
        return new double[] {(a[0] + b[0]) / 2, (a[1] + b[1]) / 2, (a[2] + b[2]) / 2};
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }
}
