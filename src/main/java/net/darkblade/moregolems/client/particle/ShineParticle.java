package net.darkblade.moregolems.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ShineParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float initialSize;
    private final float rotationSpeed;

    protected ShineParticle(ClientLevel level, double x, double y, double z,
                            double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0);
        this.sprites = sprites;

        this.lifetime = 10 + this.random.nextInt(5);

        this.quadSize = 0.2F + this.random.nextFloat() * 0.2F;

        this.initialSize = this.quadSize;

        this.gravity = 0.0F;
        this.hasPhysics = false;

        this.roll = this.random.nextFloat() * (float)Math.PI * 2.0F;
        this.oRoll = this.roll;
        this.rotationSpeed = (this.random.nextFloat() - 0.5F) * 0.05F;

        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        this.oRoll = this.roll;
        this.roll += this.rotationSpeed;

        this.setSpriteFromAge(this.sprites);

        if (this.age > this.lifetime * 0.8) {
            this.alpha *= 0.6F;
        }
    }

    @Override
    public float getQuadSize(float partialTicks) {
        float progress = ((float)this.age + partialTicks) / (float)this.lifetime;

        float scaleCurve = Mth.sin(progress * (float)Math.PI) * 1.5F;

        scaleCurve = Mth.clamp(scaleCurve, 0.0F, 2.0F);

        return this.initialSize * scaleCurve;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new ShineParticle(level, x, y, z, 0, 0, 0, this.sprites);
        }
    }
}