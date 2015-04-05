package com.temportalist.thaumicexpansion.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

/**
 * Decomposer - Drullkus
 * Created using Tabula 4.1.1
 */
public class ModelAnalyzer extends ModelBase {
    public ModelRenderer Block1;
    public ModelRenderer Frame1;
    public ModelRenderer Frame2;
    public ModelRenderer Frame3;
    public ModelRenderer Frame4;
    public ModelRenderer Frame1_1;
    public ModelRenderer Frame2_1;
    public ModelRenderer Frame3_1;
    public ModelRenderer Frame4_1;
    public ModelRenderer Frame1_2;
    public ModelRenderer Frame2_2;
    public ModelRenderer Frame3_2;
    public ModelRenderer Frame4_2;
    public ModelRenderer Arm1;
    public ModelRenderer Arm2;
    public ModelRenderer Arm3;
    public ModelRenderer Arm1_1;
    public ModelRenderer Arm2_1;
    public ModelRenderer Arm3_1;
    public ModelRenderer Arm1_2;
    public ModelRenderer Arm2_2;
    public ModelRenderer Arm3_2;
    public ModelRenderer Arm1_3;
    public ModelRenderer Arm2_3;
    public ModelRenderer Arm3_3;

    public ModelAnalyzer() {
        this.textureWidth = 64;
        this.textureHeight = 64;
        this.Frame3_1 = new ModelRenderer(this, 0, 0);
        this.Frame3_1.setRotationPoint(0.0F, 20.5F, 0.0F);
        this.Frame3_1.addBox(-8.0F, 2.5F, -8.0F, 16, 1, 1, 0.0F);
        this.setRotateAngle(Frame3_1, 0.0F, 3.141592653589793F, 0.0F);
        this.Frame3 = new ModelRenderer(this, 0, 0);
        this.Frame3.setRotationPoint(0.0F, 20.5F, 0.0F);
        this.Frame3.addBox(-8.0F, -2.5F, -8.0F, 16, 1, 1, 0.0F);
        this.setRotateAngle(Frame3, 0.0F, 3.141592653589793F, 0.0F);
        this.Arm3 = new ModelRenderer(this, 0, 0);
        this.Arm3.setRotationPoint(6.0F, 10.0F, -6.0F);
        this.Arm3.addBox(-1.0F, -4.0F, 0.0F, 1, 4, 1, 0.2F);
        this.setRotateAngle(Arm3, -0.6108652381980153F, 0.0F, -0.6108652381980153F);
        this.Arm3_3 = new ModelRenderer(this, 0, 0);
        this.Arm3_3.setRotationPoint(-6.0F, 10.0F, 6.0F);
        this.Arm3_3.addBox(0.0F, -4.0F, -1.0F, 1, 4, 1, 0.2F);
        this.setRotateAngle(Arm3_3, 0.6108652381980153F, 0.0F, 0.6108652381980153F);
        this.Frame1 = new ModelRenderer(this, 0, 0);
        this.Frame1.setRotationPoint(0.0F, 20.5F, 0.0F);
        this.Frame1.addBox(-8.0F, -2.5F, -8.0F, 16, 1, 1, 0.0F);
        this.Frame4 = new ModelRenderer(this, 1, 0);
        this.Frame4.setRotationPoint(0.0F, 20.5F, 0.0F);
        this.Frame4.addBox(-7.0F, -2.5F, -8.0F, 14, 1, 1, 0.0F);
        this.setRotateAngle(Frame4, 0.0F, -1.5707963267948966F, 0.0F);
        this.Arm2_3 = new ModelRenderer(this, 0, 0);
        this.Arm2_3.setRotationPoint(-8.15F, 14.0F, 8.15F);
        this.Arm2_3.addBox(0.5F, -5.0F, -2.5F, 2, 5, 2, 0.0F);
        this.setRotateAngle(Arm2_3, 0.2792526803190927F, 0.0F, 0.2792526803190927F);
        this.Frame2 = new ModelRenderer(this, 1, 0);
        this.Frame2.setRotationPoint(0.0F, 20.5F, 0.0F);
        this.Frame2.addBox(-7.0F, -2.5F, -8.0F, 14, 1, 1, 0.0F);
        this.setRotateAngle(Frame2, 0.0F, 1.5707963267948966F, 0.0F);
        this.Arm2_1 = new ModelRenderer(this, 0, 0);
        this.Arm2_1.setRotationPoint(-8.15F, 14.0F, -8.15F);
        this.Arm2_1.addBox(0.5F, -5.0F, 0.5F, 2, 5, 2, 0.0F);
        this.setRotateAngle(Arm2_1, -0.2792526803190927F, 0.0F, 0.2792526803190927F);
        this.Frame4_2 = new ModelRenderer(this, 0, 2);
        this.Frame4_2.setRotationPoint(0.0F, 20.5F, 0.0F);
        this.Frame4_2.addBox(7.0F, -1.5F, 7.0F, 1, 4, 1, 0.0F);
        this.Frame1_2 = new ModelRenderer(this, 0, 2);
        this.Frame1_2.setRotationPoint(0.0F, 20.5F, 0.0F);
        this.Frame1_2.addBox(7.0F, -1.5F, 7.0F, 1, 4, 1, 0.0F);
        this.setRotateAngle(Frame1_2, 0.0F, 1.5707963267948966F, 0.0F);
        this.Frame2_1 = new ModelRenderer(this, 1, 0);
        this.Frame2_1.setRotationPoint(0.0F, 20.5F, 0.0F);
        this.Frame2_1.addBox(-7.0F, 2.5F, -8.0F, 14, 1, 1, 0.0F);
        this.setRotateAngle(Frame2_1, 0.0F, 1.5707963267948966F, 0.0F);
        this.Arm1_2 = new ModelRenderer(this, 0, 0);
        this.Arm1_2.setRotationPoint(8.0F, 14.0F, 8.0F);
        this.Arm1_2.addBox(-3.0F, 0.0F, -3.0F, 3, 5, 3, 0.0F);
        this.setRotateAngle(Arm1_2, -0.17453292519943295F, 0.0F, 0.17453292519943295F);
        this.Arm2_2 = new ModelRenderer(this, 0, 0);
        this.Arm2_2.setRotationPoint(8.15F, 14.0F, 8.15F);
        this.Arm2_2.addBox(-2.5F, -5.0F, -2.5F, 2, 5, 2, 0.0F);
        this.setRotateAngle(Arm2_2, 0.2792526803190927F, 0.0F, -0.2792526803190927F);
        this.Arm1 = new ModelRenderer(this, 0, 0);
        this.Arm1.setRotationPoint(8.0F, 14.0F, -8.0F);
        this.Arm1.addBox(-3.0F, 0.0F, 0.0F, 3, 5, 3, 0.0F);
        this.setRotateAngle(Arm1, 0.17453292519943295F, 0.0F, 0.17453292519943295F);
        this.Frame1_1 = new ModelRenderer(this, 0, 0);
        this.Frame1_1.setRotationPoint(0.0F, 20.5F, 0.0F);
        this.Frame1_1.addBox(-8.0F, 2.5F, -8.0F, 16, 1, 1, 0.0F);
        this.Frame4_1 = new ModelRenderer(this, 1, 0);
        this.Frame4_1.setRotationPoint(0.0F, 20.5F, 0.0F);
        this.Frame4_1.addBox(-7.0F, 2.5F, -8.0F, 14, 1, 1, 0.0F);
        this.setRotateAngle(Frame4_1, 0.0F, -1.5707963267948966F, 0.0F);
        this.Arm1_1 = new ModelRenderer(this, 0, 0);
        this.Arm1_1.setRotationPoint(-8.0F, 14.0F, -8.0F);
        this.Arm1_1.addBox(0.0F, 0.0F, 0.0F, 3, 5, 3, 0.0F);
        this.setRotateAngle(Arm1_1, 0.17453292519943295F, 0.0F, -0.17453292519943295F);
        this.Arm3_1 = new ModelRenderer(this, 0, 0);
        this.Arm3_1.setRotationPoint(-6.0F, 10.0F, -6.0F);
        this.Arm3_1.addBox(0.0F, -4.0F, 0.0F, 1, 4, 1, 0.2F);
        this.setRotateAngle(Arm3_1, -0.6108652381980153F, 0.0F, 0.6108652381980153F);
        this.Arm1_3 = new ModelRenderer(this, 0, 0);
        this.Arm1_3.setRotationPoint(-8.0F, 14.0F, 8.0F);
        this.Arm1_3.addBox(0.0F, 0.0F, -3.0F, 3, 5, 3, 0.0F);
        this.setRotateAngle(Arm1_3, -0.17453292519943295F, 0.0F, -0.17453292519943295F);
        this.Frame2_2 = new ModelRenderer(this, 0, 2);
        this.Frame2_2.setRotationPoint(0.0F, 20.5F, 0.0F);
        this.Frame2_2.addBox(7.0F, -1.5F, 7.0F, 1, 4, 1, 0.0F);
        this.setRotateAngle(Frame2_2, 0.0F, 3.141592653589793F, 0.0F);
        this.Arm2 = new ModelRenderer(this, 0, 0);
        this.Arm2.setRotationPoint(8.15F, 14.0F, -8.15F);
        this.Arm2.addBox(-2.5F, -5.0F, 0.5F, 2, 5, 2, 0.0F);
        this.setRotateAngle(Arm2, -0.2792526803190927F, 0.0F, -0.2792526803190927F);
        this.Block1 = new ModelRenderer(this, 0, 22);
        this.Block1.setRotationPoint(0.0F, 18.5F, 0.0F);
        this.Block1.addBox(-7.5F, 0.0F, -7.5F, 15, 5, 15, 0.3F);
        this.Arm3_2 = new ModelRenderer(this, 0, 0);
        this.Arm3_2.setRotationPoint(6.0F, 10.0F, 6.0F);
        this.Arm3_2.addBox(-1.0F, -4.0F, -1.0F, 1, 4, 1, 0.2F);
        this.setRotateAngle(Arm3_2, 0.6108652381980153F, 0.0F, -0.6108652381980153F);
        this.Frame3_2 = new ModelRenderer(this, 0, 2);
        this.Frame3_2.setRotationPoint(0.0F, 20.5F, 0.0F);
        this.Frame3_2.addBox(7.0F, -1.5F, 7.0F, 1, 4, 1, 0.0F);
        this.setRotateAngle(Frame3_2, 0.0F, -1.5707963267948966F, 0.0F);
    }

    @Override
    public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) { 
        this.Frame3_1.render(f5);
        this.Frame3.render(f5);
        this.Arm3.render(f5);
        this.Arm3_3.render(f5);
        this.Frame1.render(f5);
        this.Frame4.render(f5);
        this.Arm2_3.render(f5);
        this.Frame2.render(f5);
        this.Arm2_1.render(f5);
        this.Frame4_2.render(f5);
        this.Frame1_2.render(f5);
        this.Frame2_1.render(f5);
        this.Arm1_2.render(f5);
        this.Arm2_2.render(f5);
        this.Arm1.render(f5);
        this.Frame1_1.render(f5);
        this.Frame4_1.render(f5);
        this.Arm1_1.render(f5);
        this.Arm3_1.render(f5);
        this.Arm1_3.render(f5);
        this.Frame2_2.render(f5);
        this.Arm2.render(f5);
        this.Block1.render(f5);
        this.Arm3_2.render(f5);
        this.Frame3_2.render(f5);
    }

    /**
     * This is a helper function from Tabula to set the rotation of model parts
     */
    public void setRotateAngle(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.rotateAngleX = x;
        modelRenderer.rotateAngleY = y;
        modelRenderer.rotateAngleZ = z;
    }
}
