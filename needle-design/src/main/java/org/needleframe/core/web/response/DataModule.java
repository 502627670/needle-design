package org.needleframe.core.web.response;

import java.util.ArrayList;
import java.util.List;

import org.needleframe.core.model.Module;
import org.needleframe.core.web.response.DataModuleBuilder.DataAction;
import org.needleframe.core.web.response.DataModuleBuilder.DataChild;
import org.needleframe.core.web.response.DataModuleBuilder.DataFilter;
import org.needleframe.core.web.response.DataModuleBuilder.DataProp;
import org.needleframe.core.web.response.DataModuleBuilder.DataViewProp;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataModule {
	
	private String showName;
	private String name;
	private String pk;
	private String taskProp;
	private List<DataChild> children = new ArrayList<DataChild>();
	private List<DataProp> props = new ArrayList<DataProp>();
	private List<DataViewProp> viewProps = new ArrayList<DataViewProp>();
	private List<DataFilter> filterProps = new ArrayList<DataFilter>();
	private List<DataAction> actions = new ArrayList<DataAction>();
	
	public DataModule(Module module) {
		this.name = module.getName();
		this.showName = module.getShowName();
		this.pk = module.getPk();
		this.taskProp = module.getTaskProp();
	}
	
}
