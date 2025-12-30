package net.darkblade.moregolems.sever.entity.debug;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class DebugAABB {
    public static void drawAabbEdges(ServerLevel sl, AABB box) {
        Vec3[] c = new Vec3[]{
                new Vec3(box.minX, box.minY, box.minZ), new Vec3(box.maxX, box.minY, box.minZ),
                new Vec3(box.minX, box.minY, box.maxZ), new Vec3(box.maxX, box.minY, box.maxZ),
                new Vec3(box.minX, box.maxY, box.minZ), new Vec3(box.maxX, box.maxY, box.minZ),
                new Vec3(box.minX, box.maxY, box.maxZ), new Vec3(box.maxX, box.maxY, box.maxZ)
        };
        int[][] edges = new int[][]{{0,1},{0,2},{1,3},{2,3},{4,5},{4,6},{5,7},{6,7},{0,4},{1,5},{2,6},{3,7}};
        for (int[] e : edges) drawLine(sl, c[e[0]], c[e[1]]);
    }

    private static void drawLine(ServerLevel sl, Vec3 a, Vec3 b) {
        for (int i = 0; i <= 10; i++) {
            Vec3 p = a.lerp(b, i / 10.0);
            sl.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }
}