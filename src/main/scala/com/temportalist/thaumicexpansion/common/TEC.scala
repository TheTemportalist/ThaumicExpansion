package com.temportalist.thaumicexpansion.common

import java.util
import java.util.UUID

import com.temportalist.origin.api.common.lib.Pair
import com.temportalist.origin.api.common.resource.IModDetails
import com.temportalist.origin.foundation.common.IMod
import com.temportalist.origin.foundation.common.utility.Players
import com.temportalist.origin.internal.common.handlers.RegisterHelper
import com.temportalist.thaumicexpansion.common.init.{TECBlocks, TECItems}
import com.temportalist.thaumicexpansion.common.lib.ThaumcraftHelper
import com.temportalist.thaumicexpansion.common.network.PacketGiveAspect
import com.temportalist.thaumicexpansion.server.CommandTEC
import cpw.mods.fml.common.event.{FMLInitializationEvent, FMLPostInitializationEvent, FMLPreInitializationEvent}
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.PlayerEvent
import cpw.mods.fml.common.{Mod, SidedProxy}
import cpw.mods.fml.relauncher.Side
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Items
import net.minecraft.item.crafting.IRecipe
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.util.ResourceLocation
import net.minecraftforge.oredict.ShapedOreRecipe
import thaumcraft.api.aspects.{Aspect, AspectList}
import thaumcraft.api.research.{ResearchCategories, ResearchPage, ScanResult}
import thaumcraft.common.Thaumcraft
import thaumcraft.common.config.{ConfigBlocks, ConfigItems}
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager
import thaumcraft.common.lib.research.{PlayerKnowledge, ScanManager}

/**
 *
 *
 * @author TheTemportalist
 */
@Mod(modid = TEC.MODID, name = TEC.MODNAME, version = TEC.VERSION, modLanguage = "scala",
	dependencies = "required-after:Thaumcraft@[4.2,);" +
			"required-after:origin@[8,);" +
			"after:CoFHCore@[1.7.10R3.0.0RC7,);" +
			"after:ThermalFoundation@[1.7.10R1.0.0RC7,);" +
			"after:ThermalExpansion@[1.7.10R4.0.0RC7,);"
)
object TEC extends IMod with IModDetails {

	final val MODID = "thaumicexpansion"
	final val MODNAME = "Thaumic Expansion"
	final val VERSION = "@MOD_VERSION@"
	//"@PLUGIN_VERSION@"
	final val PROXY_CLIENT = "com.temportalist.thaumicexpansion.client.ProxyClient"
	final val PROXY_SERVER = "com.temportalist.thaumicexpansion.common.ProxyCommon"

	override def getModid: String = this.MODID

	override def getModName: String = this.MODNAME

	override def getModVersion: String = this.VERSION

	override def getDetails: IModDetails = this

	@SidedProxy(clientSide = this.PROXY_CLIENT, serverSide = this.PROXY_SERVER)
	var proxy: ProxyCommon = null

	/**
	 * Maps the complexity of the aspect to the time and energy per aspect
	 */
	final val timeEnergyPerStats: List[Pair[Int, Int]] = List[Pair[Int, Int]](
		new Pair[Int, Int](20, 50),
		new Pair[Int, Int](60, 50),
		new Pair[Int, Int](100, 50)
	)
	final val aspectTiers: util.HashMap[Aspect, Integer] = new util.HashMap[Aspect, Integer]
	/**
	 * Key -> machine tier
	 * Value ->
	 * Key -> aspect complexity
	 * Value -> primary check for type of aspect
	 */
	final val complexityTierChance: Array[Array[Double]] = Array[Array[Double]](
		Array[Double](1, .5, .25),
		Array[Double](1, .7, .45),
		Array[Double](1, .9, .65),
		Array[Double](1, .9, .85)
	)

	val zeroMeta = Array[Int](0)
	val category = "THAUMICEXPANSION"

	@Mod.EventHandler
	def preInit(event: FMLPreInitializationEvent): Unit = {
		super.preInitialize(this, event, this.proxy, null, TECBlocks, TECItems)

		this.registerNetwork()
		this.registerPacket(classOf[PacketGiveAspect.Handler], classOf[PacketGiveAspect], Side.SERVER)

		RegisterHelper.registerCommand(CommandTEC)

	}

	@Mod.EventHandler
	def init(event: FMLInitializationEvent): Unit = {
		super.initialize(event, this.proxy)
	}

	@Mod.EventHandler
	def postInit(event: FMLPostInitializationEvent): Unit = {
		super.postInitialize(event, this.proxy)
		TEC.aspectTiers.put(Aspect.AIR, 1)
		TEC.aspectTiers.put(Aspect.EARTH, 1)
		TEC.aspectTiers.put(Aspect.FIRE, 1)
		TEC.aspectTiers.put(Aspect.WATER, 1)
		TEC.aspectTiers.put(Aspect.ORDER, 1)
		TEC.aspectTiers.put(Aspect.ENTROPY, 1)

		TEC.aspectTiers.put(Aspect.VOID, 2)
		TEC.aspectTiers.put(Aspect.LIGHT, 2)
		TEC.aspectTiers.put(Aspect.WEATHER, 2)
		TEC.aspectTiers.put(Aspect.MOTION, 2)
		TEC.aspectTiers.put(Aspect.COLD, 2)
		TEC.aspectTiers.put(Aspect.CRYSTAL, 2)
		TEC.aspectTiers.put(Aspect.LIFE, 2)
		TEC.aspectTiers.put(Aspect.POISON, 2)
		TEC.aspectTiers.put(Aspect.ENERGY, 2)
		TEC.aspectTiers.put(Aspect.EXCHANGE, 2)

		this.registerThaumcraftIntegration()

	}

	private final def registerThaumcraftIntegration(): Unit = {
		ResearchCategories.registerCategory(category,
			new ResourceLocation(TEC.MODID, "textures/items/opticalScanner.png"),
			new ResourceLocation("thaumcraft", "textures/gui/gui_researchback.png")
		)

		val analyzerStack = new ItemStack(TECBlocks.analyzer)
		ThaumcraftHelper.addResearchData(
			"THAUMICANALYZER", this.category, (List("GOGGLES"), null),
			analyzerStack, 0, 0,
			new AspectList().add(Aspect.AIR, 1), 1,
			Array[ResearchPage](
				new ResearchPage("tc.research_page.THAUMICANALYZER.1")
			),
			Array[IRecipe](
				new ShapedOreRecipe(analyzerStack,
					"isi", "i i", "wdw",
					Char.box('i'), "ingotIron",
					Char.box('s'), ConfigItems.itemThaumometer,
					Char.box('w'), new ItemStack(ConfigBlocks.blockMagicalLog, 1, 0),
					Char.box('d'), "gemDiamond"
				)
			),
			new AspectList().add(Aspect.MIND, 4).add(Aspect.METAL, 2)
					.add(Aspect.MECHANISM, 10).add(Aspect.MAGIC, 7),
			this.zeroMeta
		)

		val apparatusStack = new ItemStack(TECBlocks.apparatus)
		ThaumcraftHelper.addResearchData(
			"THAUMICAPPARATUS", this.category, (List("THAUMICANALYZER"), null),
			apparatusStack, 2, 0,
			new AspectList().add(Aspect.AIR, 1), 1,
			Array[ResearchPage](
				new ResearchPage("tc.research_page.THAUMICAPPARATUS.1")
			),
			Array[IRecipe](
				new ShapedOreRecipe(apparatusStack,
					"trt", "rgr", "trt",
					Char.box('t'), "ingotThaumium",
					Char.box('r'), "dustRedstone",
					Char.box('g'), "ingotGold"
				)
			),
			new AspectList().add(Aspect.MIND, 4).add(Aspect.METAL, 2)
					.add(Aspect.MECHANISM, 10).add(Aspect.MAGIC, 7),
			TEC.zeroMeta
		)

		ThaumcraftHelper.addResearchData(
			"PLAYERTRACKER", this.category, (List("THAUMICANALYZER"), null),
			TECItems.playerTracker, -1, 2,
			new AspectList().add(Aspect.AIR, 1), 1,
			Array[ResearchPage](
				new ResearchPage("tc.research_page.PLAYERTRACKER.1")
			),
			Array[IRecipe](
				new ShapedOreRecipe(TECItems.playerTracker,
					"ttt", "tbt", "ttt",
					Char.box('t'), "ingotThaumium",
					Char.box('b'), ConfigItems.itemZombieBrain
				)
			), new AspectList(), TEC.zeroMeta
		)

		ThaumcraftHelper.addResearchData(
			"DECOMPOSER", this.category, (List("THAUMICANALYZER"), null),
			TECItems.decomposer, 1, 2,
			new AspectList().add(Aspect.AIR, 1), 0,
			Array[ResearchPage](
				new ResearchPage("tc.research_page.DECOMPOSER.1")
			),
			Array[IRecipe](
				new ShapedOreRecipe(TECItems.decomposer,
					"igi", "pdt", "igi",
					Char.box('d'), new ItemStack(ConfigBlocks.blockTable, 1, 14),
					Char.box('p'), Items.diamond_pickaxe,
					Char.box('t'), ConfigItems.itemPickThaumium,
					Char.box('g'), "ingotGold",
					Char.box('i'), "ingotThaumium"
				)
			), new AspectList(), Array[Int](1)
		)

	}

	// todo save to disk
	private final val scanBuffer: util.HashMap[UUID, util.List[Pair[ScanResult, AspectList]]] =
		new util.HashMap[UUID, util.List[Pair[ScanResult, AspectList]]]

	@SubscribeEvent
	def login(event: PlayerEvent.PlayerLoggedInEvent) {
		val id: UUID = event.player.getGameProfile.getId
		if (TEC.scanBuffer.containsKey(id)) {
			val pendingScans: util.List[Pair[ScanResult, AspectList]] = TEC.scanBuffer.remove(id)
			for (i <- 0 until pendingScans.size()) {
				val buf: Pair[ScanResult, AspectList] = pendingScans.get(i)
				val scan: ScanResult = buf.getKey
				val aspects: AspectList = buf.getValue
				if (scan != null) {
					this.scanScan(event.player, scan)
				}
				if (aspects != null) TEC.addAspects(event.player, aspects)
			}
		}
	}

	def getAspects(scan: ScanResult): AspectList = {
		ThaumcraftCraftingManager.getObjectTags(
			new ItemStack(Item.getItemById(scan.id), 1, scan.meta)
		)
	}

	def scanScan(player: EntityPlayer, scan: ScanResult): Unit = {
		Thaumcraft.proxy.getResearchManager.completeScannedObject(player,
			"@" + ScanManager.generateItemHash(Item.getItemById(scan.id), scan.meta)
		)
	}

	def addScan(playerUUID: UUID, scan: ScanResult): Unit = {
		if (Players.isOnline(playerUUID)) {
			val player: EntityPlayer = Players.getPlayerOnline(playerUUID)
			this.scanScan(player, scan)
			TEC.addAspects(player, this.getAspects(scan))
		}
		else {
			if (!TEC.scanBuffer.containsKey(playerUUID))
				TEC.scanBuffer.put(playerUUID, new util.ArrayList[Pair[ScanResult, AspectList]]())
			TEC.scanBuffer.get(playerUUID).add(new Pair[ScanResult, AspectList](
				scan, this.getAspects(scan)))
		}
	}

	def addAspects(playerUUID: UUID, aspects: AspectList): Unit = {
		if (Players.isOnline(playerUUID)) {
			this.addAspects(Players.getPlayerOnline(playerUUID), aspects)
		}
		else {
			if (!TEC.scanBuffer.containsKey(playerUUID))
				TEC.scanBuffer.put(playerUUID, new util.ArrayList[Pair[ScanResult, AspectList]]())
			TEC.scanBuffer.get(playerUUID).add(new Pair[ScanResult, AspectList](
				null, aspects))
		}
	}

	def addAspects(player: EntityPlayer, aspects: AspectList): Unit = {
		if (aspects == null) return
		val pk: PlayerKnowledge = Thaumcraft.proxy.getPlayerKnowledge
		for (aspect <- aspects.getAspects) {
			if (pk.hasDiscoveredParentAspects(player.getCommandSenderName, aspect)) {
				ScanManager.checkAndSyncAspectKnowledge(
					player, aspect, aspects.getAmount(aspect)
				)
			}
		}
	}

	def hasScannedOffline(playerID: UUID, stack: ItemStack): Boolean = {
		val offlineScans: util.List[Pair[ScanResult, AspectList]] = TEC.scanBuffer.get(playerID)
		if (offlineScans != null) {
			for (i <- 0 until offlineScans.size()) {
				val scan: ScanResult = offlineScans.get(i).getKey
				if (scan.id == Item.getIdFromItem(stack.getItem)
						&& scan.meta == stack.getItemDamage) {
					return true
				}
			}
		}
		false
	}

	def getAspectTier(aspect: Aspect): Int =
		if (this.aspectTiers.containsKey(aspect)) this.aspectTiers.get(aspect) else 3

}
