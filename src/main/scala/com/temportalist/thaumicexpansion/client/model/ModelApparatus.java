package com.temportalist.thaumicexpansion.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

/**
 * Translocutor - TheTemportalist
 * Created using Tabula 4.1.1
 */
public class ModelApparatus extends ModelBase {
	public ModelRenderer Connector;
	public ModelRenderer Back;
	public ModelRenderer Arch_F;
	public ModelRenderer Arch_B;
	public ModelRenderer Arch_L;
	public ModelRenderer Arch_R;
	public ModelRenderer Arch_T;
	public ModelRenderer Face_F;
	public ModelRenderer Face_B;
	public ModelRenderer Face_L;
	public ModelRenderer Face_R;
	public ModelRenderer Face_T;

	public ModelApparatus() {
		this.textureWidth = 64;
		this.textureHeight = 32;
		this.Arch_F = new ModelRenderer(this, 5, 18);
		this.Arch_F.setRotationPoint(0.0F, -1.0F, -5.0F);
		this.Arch_F.addBox(-1.0F, -10.0F, -1.0F, 2, 10, 2, 0.0F);
		this.Face_T = new ModelRenderer(this, 43, 20);
		this.Face_T.setRotationPoint(0.0F, -12.0F, 0.0F);
		this.Face_T.addBox(-4.0F, -4.0F, -2.0F, 8, 8, 2, 0.0F);
		this.setRotateAngle(Face_T, -1.5707963267948966F, 0.0F, 0.0F);
		this.Arch_T = new ModelRenderer(this, 5, 1);
		this.Arch_T.setRotationPoint(0.0F, -1.0F, 0.0F);
		this.Arch_T.addBox(-1.0F, -12.0F, -1.0F, 2, 12, 2, 0.0F);
		this.Face_R = new ModelRenderer(this, 43, 20);
		this.Face_R.setRotationPoint(-1.0F, -6.0F, 0.0F);
		this.Face_R.addBox(-4.0F, -4.0F, -2.0F, 8, 8, 2, 0.0F);
		this.setRotateAngle(Face_R, 0.0F, 1.5707963267948966F, 0.0F);
		this.Face_L = new ModelRenderer(this, 43, 20);
		this.Face_L.setRotationPoint(1.0F, -6.0F, 0.0F);
		this.Face_L.addBox(-4.0F, -4.0F, -2.0F, 8, 8, 2, 0.0F);
		this.setRotateAngle(Face_L, 0.0F, -1.5707963267948966F, 0.0F);
		this.Face_F = new ModelRenderer(this, 43, 20);
		this.Face_F.setRotationPoint(0.0F, -6.0F, -1.0F);
		this.Face_F.addBox(-4.0F, -4.0F, -2.0F, 8, 8, 2, 0.0F);
		this.Arch_L = new ModelRenderer(this, 5, 18);
		this.Arch_L.setRotationPoint(5.0F, -1.0F, 0.0F);
		this.Arch_L.addBox(-1.0F, -10.0F, -1.0F, 2, 10, 2, 0.0F);
		this.Connector = new ModelRenderer(this, 0, 0);
		this.Connector.setRotationPoint(0.0F, 24.0F, 0.0F);
		this.Connector.addBox(-8.0F, -1.0F, -8.0F, 16, 1, 16, 0.0F);
		this.Face_B = new ModelRenderer(this, 43, 20);
		this.Face_B.setRotationPoint(0.0F, -6.0F, 1.0F);
		this.Face_B.addBox(-4.0F, -4.0F, -2.0F, 8, 8, 2, 0.0F);
		this.setRotateAngle(Face_B, 0.0F, 3.141592653589793F, 0.0F);
		this.Arch_B = new ModelRenderer(this, 5, 18);
		this.Arch_B.setRotationPoint(0.0F, -1.0F, 5.0F);
		this.Arch_B.addBox(-1.0F, -10.0F, -1.0F, 2, 10, 2, 0.0F);
		this.Back = new ModelRenderer(this, 0, 17);
		this.Back.setRotationPoint(0.0F, -1.0F, 0.0F);
		this.Back.addBox(-7.0F, -1.0F, -7.0F, 14, 1, 14, 0.0F);
		this.Arch_R = new ModelRenderer(this, 5, 18);
		this.Arch_R.setRotationPoint(-5.0F, -1.0F, 0.0F);
		this.Arch_R.addBox(-1.0F, -10.0F, -1.0F, 2, 10, 2, 0.0F);
		this.Back.addChild(this.Arch_F);
		this.Arch_T.addChild(this.Face_T);
		this.Back.addChild(this.Arch_T);
		this.Arch_R.addChild(this.Face_R);
		this.Arch_L.addChild(this.Face_L);
		this.Arch_F.addChild(this.Face_F);
		this.Back.addChild(this.Arch_L);
		this.Arch_B.addChild(this.Face_B);
		this.Back.addChild(this.Arch_B);
		this.Connector.addChild(this.Back);
		this.Back.addChild(this.Arch_R);
	}

	@Override
	public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
		this.Connector.render(f5);
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
