package com.my.dataconvert.constants;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.my.dataconvert.model.SegementFailEntity;
/**
 * @author 997620782@qq.com
 */
@Deprecated
public class SegementConstants {
	
	public final static Map<String, SegementFailEntity> failMap = new ConcurrentHashMap<String, SegementFailEntity>();
	
	
}
