package com.temportalist.thaumicexpansion.common.block;

import com.temportalist.thaumicexpansion.common.TEC;
import com.temportalist.thaumicexpansion.common.item.ItemBlockTA;
import com.temportalist.thaumicexpansion.common.lib.EnumSideTA;
import com.temportalist.thaumicexpansion.common.tile.TEThaumicAnalyzer;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.playerdata.PacketAspectPool;
import thaumcraft.common.lib.research.ResearchManager;

import java.util.List;

/**
 * @author TheTemportalist
 */
public class BlockThaumicAnalyzer extends net.minecraft.block.Block implements ITileEntityProvider {

	private final String modid, name;

	public BlockThaumicAnalyzer(Material mat, String modid, String name) {
		super(mat);
		this.modid = modid;
		this.name = name;
		this.setBlockName(name);
		GameRegistry.registerBlock(this, ItemBlockTA.class, name);
		GameRegistry.registerTileEntity(TEThaumicAnalyzer.class, TEC.MODID + ":" + "Decomposer");
		this.isBlockContainer = true;

	}

	@Override
	public String getUnlocalizedName() {
		return "tile." + this.modid + ":" + this.name;
	}

	@SideOnly(Side.CLIENT)
	/**
	 * Tiers -> 4
	 * Faces:
	 * 	0 -> bottom
	 * 	1 -> top
	 * 	2 -> front
	 * 	3 -> sides
	 */
	private IIcon[][] icons = new IIcon[4][4];

	@SideOnly(Side.CLIENT)
	@Override
	public void registerBlockIcons(IIconRegister reg) {
		String base = this.modid + ":" + this.name;
		for (int side = 0; side < 4; side++) {
			this.icons[side] = new IIcon[4];
			for (int tier = 0; tier < this.icons.length; tier++) {
				this.icons[side][tier] = reg.registerIcon(base + "/side" + side + "/" + tier);
			}
		}
		for (int i = 0; i < EnumSideTA.values().length; i++)
			EnumSideTA.values()[i].registerIcon(reg, base);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public IIcon getIcon(int side, int meta) {
		if (side == this.getDirection(meta).ordinal())
			side = 3;
		else if (side > 1)
			side = 2;
		return this.icons[side][this.getTier(meta)];
	}

	public void setTierAndDir(World world, int x, int y, int z, int tier, ForgeDirection dir) {
		world.setBlockMetadataWithNotify(x, y, z, this.getMetadata(tier, dir), 3);
	}

	public int getMetadata(int tier, ForgeDirection dir) {
		return tier * 4 + (dir.ordinal() - 2);
	}

	public int getTier(int meta) {
		return meta / 4;
	}

	public int getRotation(int meta) {
		return meta % 4 + 2;
	}

	public ForgeDirection getDirection(int meta) {
		return ForgeDirection.getOrientation(this.getRotation(meta));
	}

	@Override
	public int getRenderType() {
		return 20825; // ThermalHandler id
	}

	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int metadata) {
		return new TEThaumicAnalyzer();
	}

	@Override
	public void onBlockPlacedBy(
			World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
		int facing = MathHelper.floor_double((placer.rotationYaw * 4F) / 360F + 0.5D) & 3;
		int convertedFacing;
		switch (facing) {
			case 0:
				convertedFacing = 3;
				break;
			case 1:
				convertedFacing = 4;
				break;
			case 2:
				convertedFacing = 2;
				break;
			case 3:
				convertedFacing = 5;
				break;
			default:
				convertedFacing = 2;
				break;
		}
		this.setTierAndDir(world, x, y, z,
				this.getTier(stack.getItemDamage()),
				ForgeDirection.getOrientation(convertedFacing).getOpposite()
		);

		TEThaumicAnalyzer tile = (TEThaumicAnalyzer) world.getTileEntity(x, y, z);
		if (stack.hasTagCompound() && stack.getTagCompound().hasKey("Augments")) {
			NBTTagList augmentList = stack.getTagCompound().getTagList("Augments", 10);
			ItemStack[] augments = new ItemStack[augmentList.tagCount()];
			for (int i = 0; i < augmentList.tagCount(); i++) {
				NBTTagCompound augmentTag = augmentList.getCompoundTagAt(i);
				augments[augmentTag.getInteger("Slot")] = ItemStack
						.loadItemStackFromNBT(augmentTag);
			}
			tile.setAugments(augments);
		}
		if (placer instanceof EntityPlayer) {
			tile.setPlacedBy((EntityPlayer) placer);
		}

	}

	@Override
	protected ItemStack createStackedBlock(int meta) {
		return new ItemStack(Item.getItemFromBlock(this), 1, this.getMetadata(
				this.getTier(meta), ForgeDirection.EAST
		));
	}

	@Override
	public void getSubBlocks(Item item, CreativeTabs tab, List list) {
		for (ItemStack stack : TEC.machines) {
			((List<ItemStack>) list).add(stack);
		}
	}

	@Override
	public void breakBlock(World world, int x, int y, int z, net.minecraft.block.Block block,
			int meta) {
		super.breakBlock(world, x, y, z, block, meta);
		TEThaumicAnalyzer tile = (TEThaumicAnalyzer) world.getTileEntity(x, y, z);
		if (tile != null)
			tile.dropAllInventory(world, x, y, z);
		world.removeTileEntity(x, y, z);
	}

	@Override
	public int damageDropped(int meta) {
		return meta;
	}

	@Override
	public boolean onBlockEventReceived(World world, int x, int y, int z, int a, int b) {
		super.onBlockEventReceived(world, x, y, z, a, b);
		TileEntity tileentity = world.getTileEntity(x, y, z);
		return tileentity != null && tileentity.receiveClientEvent(a, b);
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side,
			float hitX, float hitY, float hitZ) {
		/*
		if (!world.isRemote && player.getHeldItem() != null) {
			AspectList aspects = ThaumcraftApiHelper.getObjectAspects(player.getHeldItem());
			Aspect[] aspects1 = aspects.getAspects();
			for (int i = 0; i < aspects1.length; i++) {
				Aspect aspect = aspects1[i];
				short amount = (short) aspects.getAmount(aspect);
				System.out.println(aspect.getName());
				this.addAspect((EntityPlayerMP) player, aspect, amount);
			}
		}
		*/
		player.openGui(TEC.instance, 0, world, x, y, z);

		return true;
	}

	private void addAspect(EntityPlayerMP player, Aspect aspect, short amount) {
		Thaumcraft.proxy.playerKnowledge.addAspectPool(
				player.getCommandSenderName(), aspect, amount
		);
		ResearchManager.scheduleSave(player);
		PacketHandler.INSTANCE.sendTo(
				new PacketAspectPool(
						aspect.getTag(), amount,
						Thaumcraft.proxy.playerKnowledge.getAspectPoolFor(
								player.getCommandSenderName(), aspect
						)
				), player
		);
	}

}
