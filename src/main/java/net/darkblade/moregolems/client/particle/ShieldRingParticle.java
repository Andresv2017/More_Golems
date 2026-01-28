package net.darkblade.moregolems.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ShieldRingParticle extends FloorParticle {

    private final SpriteSet sprites;
    private final float maxSize;

    protected ShieldRingParticle(ClientLevel level,
                                 double x, double y, double z,
                                 SpriteSet sprites,
                                 float maxSize) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.maxSize = maxSize;

        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;

        this.alpha = 1.0F;
        this.quadSize = 0.5F;

        this.lifetime = 24;

        this.roll = (float) Math.random() * (float) Math.PI * 2.0F;
        this.oRoll = this.roll;

        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        this.quadSize = Mth.lerp(0.4F, this.quadSize, this.maxSize);

        if (this.age++ >= this.lifetime) {
            this.remove();
        }

        else if (this.age > (this.lifetime - 8)) {
            this.alpha -= 0.15F;
            if (this.alpha < 0.0F) {
                this.alpha = 0.0F;
            }
        }

        // Importante: Si tu textura tiene muchos frames (varios anillos),
        // setSpriteFromAge intentará reproducirlos todos en 24 ticks.
        // Si ves que va muy rápido, cambia esto por: this.setSprite(this.sprites.get(0, 1));
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float delta) {
        // Renderizamos la partícula pegada al suelo y rotada
        this.renderRotatedParticle(consumer, camera, delta, false, 0.0F);
    }

    @Override
    protected int getLightColor(float partialTicks) {
        return 240; // Brilla en la oscuridad (Full bright)
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private static final float RING_DIAMETER = 3.0F;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type,
                                       ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new ShieldRingParticle(level, x, y, z, this.sprites, RING_DIAMETER);
        }
    }
}