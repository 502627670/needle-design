package org.needleframe.security.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Transient;

import lombok.Data;

@Data
public class Resource {
	
	public static enum ResourceType {
		VIEW,
		MENU,
		ACTION;
	}	
	
	@Transient
	private boolean checked;
	
	private String name;
	
	private String showName;
	
	@Enumerated(EnumType.STRING)
	private ResourceType resourceType;
	
	private String uri;
	
	private List<Resource> children = new ArrayList<Resource>();
	
}


