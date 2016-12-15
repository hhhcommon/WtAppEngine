package com.woting.appengine.searchcrawler.utils;

import com.woting.cm.core.person.persis.po.PersonPo;

public abstract class GetPersonInfo {

	public static PersonPo makePersonInfo(String contentId, String pSource) {
		if (pSource.equals("喜马拉雅")) {
			
		} else if (pSource.equals("蜻蜓")) {
			
		}
		return null;
	}
}
