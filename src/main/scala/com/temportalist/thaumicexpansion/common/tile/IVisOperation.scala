package com.temportalist.thaumicexpansion.common.tile

import com.temportalist.origin.api.common.lib.V3O
import com.temportalist.thaumicexpansion.api.common.tile.IOperation
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import thaumcraft.api.aspects.{Aspect, AspectList}
import thaumcraft.api.visnet.VisNetHandler
import thaumcraft.common.tiles.TileVisRelay

/**
 * Created by TheTemportalist on 9/1/2015.
 */
trait IVisOperation extends IOperation {

	private val demands = new AspectList()
	private val buffer = new AspectList()
	private val coefficient = 20

	protected def addCentiVisDemandPerTick(aspect: Aspect, amt: Int): Unit = {
		//demands.add(aspect, amt)
		this.demands.add(this.getPrimalBreakdown(aspect, amt))
	}

	private def getPrimalBreakdown(aspect: Aspect, amt: Int): AspectList = {
		val aspects = new AspectList()
		if (aspect.isPrimal) aspects.add(aspect, amt)
		else aspect.getComponents.foreach(aspectComp => {
			aspects.add(this.getPrimalBreakdown(aspectComp, amt))
		})
		aspects
	}

	def getPosition: (World, V3O)

	override def tick(tile: TileEntity): Unit = {
		var ticksToAdd = 1
		tile match {
			case visRelay: TileVisRelay =>
				val pos = this.getPosition
				if (pos != null && this.demands.size() > 0) {
					var canBoost = true
					this.demands.getAspects.foreach(aspect => {
						// filling the buffer
						val amountOfAspectNeeded =
							this.demands.getAmount(aspect) - this.buffer.getAmount(aspect)
						if (amountOfAspectNeeded > 0) {
							val drained = VisNetHandler.drainVis(pos._1,
								pos._2.x_i(), pos._2.y_i(), pos._2.z_i(),
								aspect, amountOfAspectNeeded)
							//println(aspect.getName + ":" + drained)
							this.buffer.add(aspect, drained)
						}

						//println(aspect.getName + ":" + this.buffer.getAmount(aspect) + ":" +
						//		this.demands.getAmount(aspect))

						if (this.buffer.getAmount(aspect) < this.demands.getAmount(aspect))
							canBoost = false
					})
					if (canBoost && this.coefficient <= this.maxTicks - this.currentTicks)
						ticksToAdd = this.coefficient
				}
			case _ =>
		}
		this.currentTicks += ticksToAdd
	}

}
