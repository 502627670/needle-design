package org.needleframe.core.model;

import lombok.Data;

@Data
public class Checkable<T> {
	
	private boolean checked = false;
	
	private T data;
	
}
