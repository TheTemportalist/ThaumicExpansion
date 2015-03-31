package com.temportalist.thaumicexp.server;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.research.PlayerKnowledge;

import java.util.List;

/**
 * @author TheTemportalist
 */
public class CommandTEC extends CommandBase {

	@Override
	public String getCommandName() {
		return "tec";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "tec <clearKnow [playerName] >";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (args.length > 0) {
			String subCommand = args[0];

			if (subCommand.equals("clearKnow")) {
				EntityPlayer player = null;
				if (args.length > 1) {
					List<EntityPlayer> players = (List<EntityPlayer>)
							MinecraftServer.getServer().getConfigurationManager().playerEntityList;
					for (EntityPlayer inGamePlayer : players) {
						if (inGamePlayer.getCommandSenderName().equals(args[1])) {
							player = inGamePlayer;
							break;
						}
					}
				}
				else if (sender instanceof EntityPlayer) {
					player = (EntityPlayer) sender;
				}
				if (player != null) {

					PlayerKnowledge knowledge = Thaumcraft.proxy.getPlayerKnowledge();
					String pName = player.getCommandSenderName();
					knowledge.aspectsDiscovered.put(pName, new AspectList());
					knowledge.objectsScanned.put(pName, null);
					knowledge.entitiesScanned.put(pName, null);
					knowledge.phenomenaScanned.put(pName, null);

				}
			}

		}
	}

}
