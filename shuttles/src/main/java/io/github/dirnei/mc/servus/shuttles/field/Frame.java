package io.github.dirnei.mc.servus.shuttles.field;

/**
 * An affine transform of model space: a 3 by 3 matrix {@code m} (rows) and a
 * translation {@code t}, mapping {@code p} to {@code m p + t}. The matrix may
 * scale as well as turn. Angles are radians, right handed, as Minecraft's
 * {@code Axis.XP}, {@code YP} and {@code ZP}. Pure Java.
 */
public record Frame(double[][] m, double[] t) {

    public static final Frame IDENTITY = new Frame(new double[][] {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}}, new double[] {0, 0, 0});

    public static Frame translation(double x, double y, double z) {
        return new Frame(IDENTITY.m, new double[] {x, y, z});
    }

    public static Frame translation(double[] p) {
        return translation(p[0], p[1], p[2]);
    }

    public static Frame rotX(double a) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        return new Frame(new double[][] {{1, 0, 0}, {0, c, -s}, {0, s, c}}, new double[3]);
    }

    public static Frame rotY(double a) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        return new Frame(new double[][] {{c, 0, s}, {0, 1, 0}, {-s, 0, c}}, new double[3]);
    }

    public static Frame rotZ(double a) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        return new Frame(new double[][] {{c, -s, 0}, {s, c, 0}, {0, 0, 1}}, new double[3]);
    }

    public static Frame scale(double x, double y, double z) {
        return new Frame(new double[][] {{x, 0, 0}, {0, y, 0}, {0, 0, z}}, new double[3]);
    }

    /** The turn {@code r} about the pivot instead of the origin. */
    public static Frame about(double[] pivot, Frame r) {
        return translation(pivot).then(r).then(translation(-pivot[0], -pivot[1], -pivot[2]));
    }

    /**
     * A frame at {@code a} whose +z runs to {@code c}, +x level where it can,
     * so a box from z 0 to the distance lies along the segment.
     */
    public static Frame along(double[] a, double[] c) {
        double[] d = {c[0] - a[0], c[1] - a[1], c[2] - a[2]};
        double len = Math.max(1e-6, Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]));
        double[] f = {d[0] / len, d[1] / len, d[2] / len};
        double[] ref = Math.abs(f[1]) > 0.95 ? new double[] {1, 0, 0} : new double[] {0, 1, 0};
        double[] r = cross(ref, f);
        double rl = Math.sqrt(r[0] * r[0] + r[1] * r[1] + r[2] * r[2]);
        r = new double[] {r[0] / rl, r[1] / rl, r[2] / rl};
        double[] u = cross(f, r);
        return new Frame(new double[][] {{r[0], u[0], f[0]}, {r[1], u[1], f[1]}, {r[2], u[2], f[2]}}, a.clone());
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    /** This frame applied after {@code inner}: points go through {@code inner} first. */
    public Frame then(Frame inner) {
        double[][] r = new double[3][3];
        double[] tr = new double[3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                r[i][j] = m[i][0] * inner.m[0][j] + m[i][1] * inner.m[1][j] + m[i][2] * inner.m[2][j];
            }
            tr[i] = m[i][0] * inner.t[0] + m[i][1] * inner.t[1] + m[i][2] * inner.t[2] + t[i];
        }
        return new Frame(r, tr);
    }

    public double[] apply(double x, double y, double z) {
        return new double[] {
                m[0][0] * x + m[0][1] * y + m[0][2] * z + t[0],
                m[1][0] * x + m[1][1] * y + m[1][2] * z + t[1],
                m[2][0] * x + m[2][1] * y + m[2][2] * z + t[2]};
    }

    public double[] apply(double[] p) {
        return apply(p[0], p[1], p[2]);
    }
}
