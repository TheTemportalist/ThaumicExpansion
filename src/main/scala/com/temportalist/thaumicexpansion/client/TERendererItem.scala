package com.temportalist.thaumicexpansion.client

import com.temportalist.origin.wrapper.client.render.TERenderer
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.client.IItemRenderer
import net.minecraftforge.client.IItemRenderer.{ItemRenderType, ItemRendererHelper}
import org.lwjgl.opengl.GL11

/**
 *
 *
 * @author TheTemportalist 4/5/15
 */
trait TERendererItem extends TERenderer with IItemRenderer {

	override def handleRenderType(item: ItemStack, `type`: ItemRenderType): Boolean = true

	override def shouldUseRenderHelper(`type`: ItemRenderType, item: ItemStack,
			helper: ItemRendererHelper): Boolean = true

	override def renderItem(iType: ItemRenderType, item: ItemStack, data: AnyRef*): Unit = {
		GL11.glPushMatrix()

		if (iType == ItemRenderType.INVENTORY) GL11.glTranslated(0, -0.1, 0)
		else if (iType == ItemRenderType.EQUIPPED_FIRST_PERSON) GL11.glTranslated(0.5, 0.5, 0.5)
		else if (iType == ItemRenderType.EQUIPPED) GL11.glTranslated(0.5, 0.5, 0.5)

		this.renderTileEntityAt(this.getRenderingTileItem, -0.5, -0.5, -0.5, 0)

		GL11.glPopMatrix()
	}

	def getRenderingTileItem(): TileEntity

}
