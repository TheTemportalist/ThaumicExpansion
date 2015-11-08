package com.temportalist.thaumicexpansion.common.lib

import com.temportalist.thaumicexpansion.common.JavaHelper
import cpw.mods.fml.common.registry.GameRegistry
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.IRecipe
import thaumcraft.api.ThaumcraftApi
import thaumcraft.api.aspects.AspectList
import thaumcraft.api.research.{ResearchItem, ResearchPage}

import scala.collection.mutable.ListBuffer

/**
 * Created by TheTemportalist on 11/7/2015.
 */
object ThaumcraftHelper {

	def getAspectListAsString(list: AspectList): String = {
		var ret = ""
		list.getAspects.foreach(aspect => {
			if (aspect == null) list.remove(null)
			else ret += aspect.getName + ":" + list.getAmount(aspect) + ","
		})
		"AspectList{" + ret + "}"
	}

	/**
	 * Adds a research entry (RESEARCH) and object aspect tags (ASPECTS)
	 * @param key RESEARCH The key for the research entry
	 * @param category RESEARCH The category for the research entry
	 * @param parents RESEARCH Parents which are required before you can discover the research.
	 *                This is a tuple of (visible parents, hidden parents).
	 * @param objectStack RESEARCH|ASPECTS The stack which is used for both visually for the
	 *                    research and for the tags for aspects.
	 * @param row RESEARCH The row where the research is located
	 * @param col RESEARCH The column where the research is located
	 * @param researchAspects RESEARCH The aspects which are required to obtain the research
	 * @param complexity RESEARCH The complexity of the research. Used for difficult when scribing?
	 * @param pages RESEARCH Pages within the research entry. Added before recipes.
	 * @param recipes RESEARCH Any recipes which should be added after normal pages.
	 *                These are automatically registered.
	 * @param objectAspects ASPECTS Aspects to be registered to the objectStack.
	 * @param possibleMetadata ASPECTS Any metadata for the objectStack.
	 * @return The ResearchItem
	 */
	def addResearchData(
			key: String, category: String, parents: (List[String], List[String]),
			objectStack: ItemStack,
			row: Int, col: Int,
			researchAspects: AspectList, complexity: Int,
			pages: Array[ResearchPage],
			recipes: Array[IRecipe],
			objectAspects: AspectList,
			possibleMetadata: Array[Int]): ResearchItem = {

		ThaumcraftApi.registerObjectTag(objectStack, possibleMetadata, objectAspects)

		val research = new ResearchItem(key, category, researchAspects,
			row, col, complexity, objectStack)

		val allPages: ListBuffer[ResearchPage] = new ListBuffer[ResearchPage]()
		pages.foreach(allPages.+=)
		recipes.foreach(recipe => {
			GameRegistry.addRecipe(recipe)
			allPages += new ResearchPage(recipe)
		})

		val researchItem = JavaHelper.setResearchPages(research, allPages.toArray)
		if (parents != null) {
			if (parents._1 != null) researchItem.parents = parents._1.toArray
			if (parents._2 != null) researchItem.parentsHidden = parents._2.toArray
		}
		researchItem.registerResearchItem()
	}

}
