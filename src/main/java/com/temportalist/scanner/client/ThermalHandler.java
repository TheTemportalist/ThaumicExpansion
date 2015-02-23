package com.temportalist.scanner.client;

import com.temportalist.scanner.common.BlockDecomposer;
import com.temportalist.scanner.common.EnumDecomposerSide;
import com.temportalist.scanner.common.TEDecomposer;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.common.FMLLog;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;
import org.lwjgl.opengl.GL11;

/**
 * @author TheTemportalist
 */
public class ThermalHandler implements ISimpleBlockRenderingHandler {

	@Override
	public int getRenderId() {
		return 20825;
	}

	@Override
	public boolean shouldRender3DInInventory(int modelId) {
		return true;
	}

	private void renderFace(RenderBlocks renderer, ForgeDirection dir, Block block, int x, int y,
			int z, IIcon icon) {
		if (icon == null)
			FMLLog.info("[Scanner] Error null icon");
		else
			switch (dir) {
				case DOWN:
					renderer.renderFaceYNeg(block, x, y, z, icon);
					break;
				case UP:
					renderer.renderFaceYPos(block, x, y, z, icon);
					break;
				case NORTH:
					renderer.renderFaceZNeg(block, x, y, z, icon);
					break;
				case EAST:
					renderer.renderFaceXPos(block, x, y, z, icon);
					break;
				case SOUTH:
					renderer.renderFaceZPos(block, x, y, z, icon);
					break;
				case WEST:
					renderer.renderFaceXNeg(block, x, y, z, icon);
					break;
				default:
					FMLLog.info("Not rendering for side " + dir);
					break;
			}
	}

	@Override
	public void renderInventoryBlock(Block block, int metadata, int modelId,
			RenderBlocks renderer) {
		Tessellator tess = Tessellator.instance;

		block.setBlockBoundsForItemRender();
		GL11.glTranslatef(-0.5f, -0.5f, -0.5f);

		ForgeDirection faceDir = ForgeDirection.EAST;
		metadata = ((BlockDecomposer) block).getMetadata(
				((BlockDecomposer) block).getTier(metadata),
				faceDir
		);
		for (int i = 0; i < ForgeDirection.VALID_DIRECTIONS.length; i++) {
			ForgeDirection dir = ForgeDirection.VALID_DIRECTIONS[i];
			tess.startDrawingQuads();
			tess.setNormal(dir.offsetX, dir.offsetY, dir.offsetZ);
			this.renderFace(renderer, dir, block, 0, 0, 0, block.getIcon(i, metadata));
			if (dir != faceDir)
				this.renderFace(renderer, dir, block, 0, 0, 0,
						EnumDecomposerSide.EMPTY.getIcon(dir.ordinal())
				);
			tess.draw();
		}

		GL11.glTranslatef(0.5F, 0.5F, 0.5F);

	}

	@Override
	public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block,
			int modelId, RenderBlocks renderer) {
		int rawColors = block.colorMultiplier(world, x, y, z);
		float blockColorRed = (rawColors >> 16 & 0xff) / 255F;
		float blockColorGreen = (rawColors >> 8 & 0xff) / 255F;
		float blockColorBlue = (rawColors & 0xff) / 255F;

		TEDecomposer tile = (TEDecomposer) world.getTileEntity(x, y, z);

		renderer.enableAO = false;
		Tessellator tess = Tessellator.instance;
		boolean didRender = false;
		int brightness = block.getMixedBrightnessForBlock(world, x, y, z);

		float bottomWeight = 0.5F;
		float topWeight = 1.0F;
		float frontWeight = 0.8F;
		float sideWeight = 0.6F;
		float var14 = topWeight * blockColorRed;
		float var15 = topWeight * blockColorGreen;
		float var16 = topWeight * blockColorBlue;
		float var17 = bottomWeight;
		float var18 = frontWeight;
		float var19 = sideWeight;
		float var20 = bottomWeight;
		float var21 = frontWeight;
		float var22 = sideWeight;
		float var23 = bottomWeight;
		float var24 = frontWeight;
		float var25 = sideWeight;

		for (int i = 0; i < ForgeDirection.VALID_DIRECTIONS.length; i++) {
			ForgeDirection dir = ForgeDirection.VALID_DIRECTIONS[i];
			int x1 = x + dir.offsetX;
			int y1 = y + dir.offsetY;
			int z1 = z + dir.offsetZ;
			if (renderer.renderAllFaces || block.shouldSideBeRendered(world, x1, y1, z1, i)) {
				tess.setBrightness(renderer.renderMinY > 0d ? brightness :
						block.getMixedBrightnessForBlock(world, x1, y1, z1));

				switch (dir) {
					case DOWN:
						tess.setColorOpaque_F(var17, var20, var23);
						break;
					case UP:
						tess.setColorOpaque_F(var14, var15, var16);
						break;
					case NORTH:
						tess.setColorOpaque_F(var18, var21, var24);
						break;
					case EAST:
						tess.setColorOpaque_F(var19, var22, var25);
						break;
					case SOUTH:
						tess.setColorOpaque_F(var18, var21, var24);
						break;
					case WEST:
						tess.setColorOpaque_F(var19, var22, var25);
						break;
					default:
						break;
				}

				IIcon[] icons = tile.getIcons(dir);
				for (int iconIndex = 0; iconIndex < icons.length; iconIndex++)
					if (icons[iconIndex] != null)
						this.renderFace(renderer, dir, block, x, y, z, icons[iconIndex]);
					else
						FMLLog.info("ERROR ICON INDEX " + iconIndex + " ON SIDE " + dir.ordinal()
								+ " is NULL");
				didRender = true;
			}
		}

		return didRender;
	}

}
