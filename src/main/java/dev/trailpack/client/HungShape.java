package dev.trailpack.client;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 도구걸이에 걸 아이템 모델의 실제 형태(꼭짓점 분포)를 재서, 손 변환과 상관없이 걸 자세를 정한다.
 * <ul>
 *   <li>long: 가장 긴 축, 머리(단면이 넓은 쪽)를 + 로</li>
 *   <li>thin: 가장 얇은 축 (평평한 그림이면 그림의 면 방향)</li>
 *   <li>center: 각 축 방향 범위의 한가운데 (모델 좌표 0~1)</li>
 * </ul>
 * 모델마다 한 번만 계산해 캐시한다 (렌더 스레드 전용).
 */
public record HungShape(Vector3f center, Vector3f longAxis, Vector3f thinAxis, float length, float thickness) {

    private static final Map<BakedModel, HungShape> CACHE = new WeakHashMap<>();
    private static final HungShape NONE = new HungShape(new Vector3f(), new Vector3f(), new Vector3f(), 0F, 0F);

    /** 모양을 잴 수 없으면(면이 없는 전용 렌더러 등) null. */
    @Nullable
    public static HungShape of(BakedModel model, ItemStack stack) {
        HungShape shape = CACHE.computeIfAbsent(model, m -> measure(m, stack));
        return shape == NONE ? null : shape;
    }

    private static HungShape measure(BakedModel model, ItemStack stack) {
        List<Vector3f> points = new ArrayList<>();
        RandomSource rand = RandomSource.create(42L);
        for (BakedModel pass : model.getRenderPasses(stack, true)) {
            for (Direction dir : Direction.values()) collect(pass.getQuads(null, dir, rand), points);
            collect(pass.getQuads(null, null, rand), points);
        }
        if (points.size() < 4) return NONE;

        // 무게중심과 공분산
        Vector3f mean = new Vector3f();
        for (Vector3f p : points) mean.add(p);
        mean.div(points.size());
        double[][] cov = new double[3][3];
        for (Vector3f p : points) {
            double[] d = {p.x - mean.x, p.y - mean.y, p.z - mean.z};
            for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) cov[i][j] += d[i] * d[j];
        }
        double[][] vec = new double[3][3];
        double[] val = eigen(cov, vec);

        int iLong = 0, iThin = 0;
        for (int i = 1; i < 3; i++) {
            if (val[i] > val[iLong]) iLong = i;
            if (val[i] < val[iThin]) iThin = i;
        }
        if (iLong == iThin) iThin = (iLong + 1) % 3;
        Vector3f longAxis = column(vec, iLong);
        Vector3f thinAxis = column(vec, iThin);
        thinAxis.sub(new Vector3f(longAxis).mul(thinAxis.dot(longAxis))).normalize(); // 직교 보정

        // 각 축 방향 범위 → 가운데
        Vector3f mid = new Vector3f(longAxis).cross(thinAxis);
        float[] lo = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        float[] hi = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        Vector3f[] axes = {longAxis, thinAxis, mid};
        Vector3f tmp = new Vector3f();
        for (Vector3f p : points) {
            tmp.set(p).sub(mean);
            for (int i = 0; i < 3; i++) {
                float t = tmp.dot(axes[i]);
                lo[i] = Math.min(lo[i], t);
                hi[i] = Math.max(hi[i], t);
            }
        }
        Vector3f center = new Vector3f(mean);
        for (int i = 0; i < 3; i++) center.add(new Vector3f(axes[i]).mul((lo[i] + hi[i]) / 2F));

        // 머리 판정: 긴 축을 반으로 나눠, 축에서 가장 멀리 벗어난 점이 있는 쪽(단면이 넓은 쪽)이 머리
        float half = (lo[0] + hi[0]) / 2F;
        float spreadPos = 0F, spreadNeg = 0F;
        for (Vector3f p : points) {
            tmp.set(p).sub(mean);
            float along = tmp.dot(longAxis);
            float side = (float) Math.hypot(tmp.dot(thinAxis), tmp.dot(mid));
            if (along >= half) spreadPos = Math.max(spreadPos, side);
            else spreadNeg = Math.max(spreadNeg, side);
        }
        if (spreadNeg > spreadPos) longAxis.negate();

        return new HungShape(center, longAxis, thinAxis, hi[0] - lo[0], hi[1] - lo[1]);
    }

    private static void collect(List<BakedQuad> quads, List<Vector3f> out) {
        for (BakedQuad quad : quads) {
            int[] v = quad.getVertices();
            int stride = v.length / 4;
            for (int i = 0; i < 4; i++) {
                int o = i * stride;
                out.add(new Vector3f(Float.intBitsToFloat(v[o]), Float.intBitsToFloat(v[o + 1]),
                        Float.intBitsToFloat(v[o + 2])));
            }
        }
    }

    private static Vector3f column(double[][] m, int c) {
        return new Vector3f((float) m[0][c], (float) m[1][c], (float) m[2][c]).normalize();
    }

    /** 대칭 3×3 행렬의 야코비 고유분해. 고윳값을 돌려주고 vec 의 열에 고유벡터를 채운다. */
    private static double[] eigen(double[][] src, double[][] vec) {
        double[][] a = new double[3][3];
        for (int i = 0; i < 3; i++) {
            System.arraycopy(src[i], 0, a[i], 0, 3);
            for (int j = 0; j < 3; j++) vec[i][j] = i == j ? 1 : 0;
        }
        for (int sweep = 0; sweep < 50; sweep++) {
            double off = Math.abs(a[0][1]) + Math.abs(a[0][2]) + Math.abs(a[1][2]);
            if (off < 1e-12) break;
            for (int p = 0; p < 2; p++) {
                for (int q = p + 1; q < 3; q++) {
                    if (Math.abs(a[p][q]) < 1e-15) continue;
                    double theta = (a[q][q] - a[p][p]) / (2 * a[p][q]);
                    double t = Math.signum(theta) / (Math.abs(theta) + Math.sqrt(theta * theta + 1));
                    if (theta == 0) t = 1;
                    double c = 1 / Math.sqrt(t * t + 1), s = t * c;
                    for (int k = 0; k < 3; k++) { // A = J^T A J
                        double akp = a[k][p], akq = a[k][q];
                        a[k][p] = c * akp - s * akq;
                        a[k][q] = s * akp + c * akq;
                    }
                    for (int k = 0; k < 3; k++) {
                        double apk = a[p][k], aqk = a[q][k];
                        a[p][k] = c * apk - s * aqk;
                        a[q][k] = s * apk + c * aqk;
                    }
                    for (int k = 0; k < 3; k++) {
                        double vkp = vec[k][p], vkq = vec[k][q];
                        vec[k][p] = c * vkp - s * vkq;
                        vec[k][q] = s * vkp + c * vkq;
                    }
                }
            }
        }
        return new double[]{a[0][0], a[1][1], a[2][2]};
    }
}
