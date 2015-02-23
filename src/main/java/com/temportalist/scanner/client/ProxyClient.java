package com.temportalist.scanner.client;

import com.temportalist.scanner.common.ProxyCommon;
import cpw.mods.fml.client.registry.RenderingRegistry;

/**
 * @author TheTemportalist
 */
public class ProxyClient extends ProxyCommon {

	@Override
	public void register() {
		RenderingRegistry.registerBlockHandler(new ThermalHandler());
	}

}
