package org.needleframe.context;

import java.util.ArrayList;
import java.util.List;

import org.needleframe.AbstractContextService;

import lombok.Getter;

@Getter
public class AppContextServiceHolder {
	
	public static List<AbstractContextService> services = new ArrayList<AbstractContextService>();
	
}
