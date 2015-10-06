package com.temportalist.thaumicexpansion.common;

import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchPage;

/**
 * Created by TheTemportalist on 8/20/2015.
 */
public class JavaHelper {

	public static ResearchItem setResearchPages(ResearchItem item, ResearchPage[] pages) {
		item.setPages(pages);
		return item;
	}

}
