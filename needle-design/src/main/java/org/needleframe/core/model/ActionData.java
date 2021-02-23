package org.needleframe.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class ActionData {
	
	private String module;
	
	private List<Map<String,Object>> data = new ArrayList<Map<String,Object>>();
	
}
