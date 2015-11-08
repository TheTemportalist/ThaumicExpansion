package com.temportalist.thaumicexpansion.common.tile

import com.temportalist.origin.api.common.inventory.IInv
import com.temportalist.origin.api.common.utility.NBTHelper
import com.temportalist.thaumicexpansion.api.common.tile.{IOperation, IOperator}
import com.temportalist.thaumicexpansion.common.TEC
import net.minecraft.item.ItemStack
import net.minecraft.nbt.{NBTTagCompound, NBTTagList}
import net.minecraft.tileentity.TileEntity

import scala.collection.mutable.ListBuffer

/**
 * Created by TheTemportalist on 11/7/2015.
 */
trait TileOperator extends TileEntity with IInv with IOperator {

	private val operations = ListBuffer[IOperation]()

	final def startOperation(op: IOperation): Unit = {
		op.start(this)
		this.operations += op
	}

	final def stopOperation(op: IOperation): Unit = {
		op.stop()
		this.operations -= op
	}

	def canOperationRun(operation: IOperation): Boolean = true

	def shouldOperationRepeat(operation: IOperation): Boolean = false

	// ~~~~~~~~~~~~~~~~~~~~~~~~ Abstracts ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	def getInputSlot: Int

	def getOutputSlot: Int

	def updateOperator(): Unit

	def onOperationFinished(operation: IOperation): Unit

	// ~~~~~~~~~~~~~~~~~~~~~~~~ IOperator ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	override final def getInput: ItemStack = this.getStackInSlot(this.getInputSlot)

	override final def getOutput: ItemStack = this.getStackInSlot(this.getOutputSlot)

	override final def onOperationCompletion(operation: IOperation): Unit = {
		if (this.shouldOperationRepeat(operation)) operation.start(this)
		else this.operations -= operation
		this.onOperationFinished(operation)
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~ TileEntity ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	override def updateEntity(): Unit = {
		this.operations.foreach(operation => {
			if (this.canOperationRun(operation)) operation.onUpdate(this)
		})
		this.updateOperator()
	}

	override def writeToNBT(tagCom: NBTTagCompound): Unit = {
		// write all the prior data
		super.writeToNBT(tagCom)
		// write the inventory
		this.writeNBT_Inv(tagCom, "Inventory")

		// create a taglist to store all the operations
		val operationsTagList = new NBTTagList
		// iterate through each operation
		this.operations.foreach(operation => {
			val operationTagCom = new NBTTagCompound
			// store operation data for recreation on read
			operationTagCom.setString("class", operation.getClass.getCanonicalName)
			operation.writeTo(operationTagCom)
			// append to operations tag list
			operationsTagList.appendTag(operationTagCom)
		})
		// save tag list
		tagCom.setTag("Operations", operationsTagList)

	}

	override def readFromNBT(tagCom: NBTTagCompound): Unit = {
		// read all the prior data
		super.readFromNBT(tagCom)
		// read the inventory
		this.readNBT_Inv(tagCom, "Inventory")

		// clear operations list
		this.operations.clear()

		// get saved operation tags
		val operationsTagList = NBTHelper.getTagList[NBTTagCompound](tagCom, "Operations")
		for (tagIndex <- 0 until operationsTagList.tagCount()) {
			val operationTagCom = operationsTagList.getCompoundTagAt(tagIndex)

			try {
				// reconstruct operation with class and NBT
				val operation = Class.forName(operationTagCom.getString("class"))
						.getConstructor().newInstance().asInstanceOf[IOperation]
				operation.readFrom(operationTagCom)
				// add operation to operations list
				this.operations += operation
			}
			catch {
				case e: Exception =>
					TEC.log("Could not reconstruct IOperation class...")
					e.printStackTrace()
			}

		}

	}

}
