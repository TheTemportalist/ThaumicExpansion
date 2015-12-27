package com.temportalist.thaumicexpansion.common.tile

import cofh.api.energy.IEnergyProvider
import com.temportalist.origin.api.common.lib.{BlockState, V3O}
import com.temportalist.origin.api.common.utility.Stacks
import com.temportalist.origin.foundation.common.network.PacketTileCallback
import com.temportalist.origin.foundation.common.tile.IPacketCallback
import com.temportalist.origin.internal.common.Origin
import com.temportalist.thaumicexpansion.api.cofh.TraitEnergyReceiver
import com.temportalist.thaumicexpansion.api.common.tile.IEnergable
import cpw.mods.fml.relauncher.Side
import net.minecraft.init.Blocks
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.ForgeDirection

/**
 *
 *
 * @author TheTemportalist
 */
class TEApparatus extends TileEntity with TraitEnergyReceiver with IPacketCallback {

	var activeSides: Array[Boolean] = Array[Boolean](false, false, false, false, false, false)
	var connectedCoord: V3O = null

	override def getMaxEnergy: Int = 16000 // 1 coal

	def getFacing: Int = {
		// world can be null if tile is the dummy inside of an item render
		if (this.worldObj == null) 0
		else this.getBlockMetadata
	}

	override def updateEntity(): Unit = {
		super.updateEntity()

		if (this.connectedCoord == null) this.refreshConnected()

		if (this.connectedCoord.getBlock(this.getWorldObj) == Blocks.air) {
			val thisVec: V3O = new V3O(this)
			Stacks.spawnItemStack(this.getWorldObj, thisVec, new BlockState(this),
				this.getWorldObj.rand, 10)
			thisVec.setBlockToAir(this.getWorldObj)
			return
		}

		val amountInTicks: Int = this.getEnergy / 10
		if (amountInTicks > 0) {
			val maxAmountPerTick: Int = 20
			val amt: Int = Math.min(amountInTicks, maxAmountPerTick)
			this.connectedCoord.getTile(this.getWorldObj) match {
				//case tile: TileAlchemyFurnace =>
				/*
				val canSmelt: Method = classOf[TileAlchemyFurnace].getDeclaredMethod("canSmelt")
				canSmelt.setAccessible(true)
				if (tile.furnaceCookTime > 0 || canSmelt.invoke(tile).asInstanceOf[Boolean]) {
					//println("invoking")
					tile.furnaceBurnTime += amt
					//tile.currentItemBurnTime += 1
					this.storage.extractEnergy(amt, true)
				}
				*/
				case energable: IEnergable =>
					if (energable.getEnergy + amt <= energable.getMaxEnergy) {
						if (energable.addEnergy(amt, doOp = false) > 0) {
							energable.addEnergy(amt, doOp = true)
							this.storage.setEnergyStored((this.getEnergy / 10 - amt) * 10)
							new PacketTileCallback(this).add("EnergyAndThat").add(this.getEnergy).
									add(new V3O(energable)).add(energable.getEnergy).
									sendToDimension(Origin, this.worldObj.provider.dimensionId)
						}
					}
				case _ =>
			}
		}

	}

	def isValidConnectorTile(thatTile: TileEntity): Boolean =
		TEApparatus.isValidConnectorTile(thatTile)

	def getActiveSides: Array[Boolean] = this.activeSides

	def checkAllSides(): Unit = {
		val thisVec: V3O = new V3O(this)
		for (dir: ForgeDirection <- ForgeDirection.VALID_DIRECTIONS) this.checkSide(thisVec, dir)
	}

	def checkSide(vec: V3O, coord: V3O): Unit = this.checkSide(vec, (vec - coord).getDir)

	def checkSide(vec: V3O, dir: ForgeDirection): Unit = {
		val preCheck: Boolean = this.activeSides(dir.ordinal())
		this.activeSides(dir.ordinal()) =
				(vec + dir).getTile(this.getWorldObj) match {
					case provider: IEnergyProvider => true
					case _ => false
				}
		if (this.activeSides(dir.ordinal()) != preCheck) {
			new PacketTileCallback(this).add("SideChange").add(dir.ordinal()).
					add(this.activeSides(dir.ordinal())).
					sendToDimension(Origin, this.worldObj.provider.dimensionId)
		}
	}

	def refreshConnected(): Unit =
		this.connectedCoord = new V3O(this) + ForgeDirection.getOrientation(this.getBlockMetadata)

	override def packetCallback(packet: PacketTileCallback, side: Side): Unit = {
		packet.get[String] match {
			case "SideChange" =>
				this.activeSides(packet.get[Int]) = packet.get[Boolean]
			case "EnergyAndThat" =>
				this.storage.setEnergyStored(packet.get[Int])
				packet.get[V3O].getTile(this.getWorldObj) match {
					case energable: IEnergable =>
						energable.setEnergy(packet.get[Int])
					case _ =>
				}
			case _ =>
		}
	}

	override def writeToNBT(tagCom: NBTTagCompound): Unit = {
		super.writeToNBT(tagCom)
		this.storage.writeToNBT(tagCom)
	}

	override def readFromNBT(tagCom: NBTTagCompound): Unit = {
		super.readFromNBT(tagCom)
		this.storage.readFromNBT(tagCom)
	}

}

object TEApparatus {
	def isValidConnectorTile(thatTile: TileEntity): Boolean = {
		thatTile match {
			case tile: IEnergable => true
			//case tile: TileAlchemyFurnace => true
			case _ => false
		}
	}
}
