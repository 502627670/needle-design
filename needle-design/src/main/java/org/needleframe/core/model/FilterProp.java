package org.needleframe.core.model;

import java.util.ArrayList;
import java.util.List;

import org.needleframe.core.model.ModuleProp.Select;
import org.needleframe.core.model.ViewFilter.Op;
import org.springframework.beans.BeanUtils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FilterProp implements Cloneable {            // 比如Permission.role.name 或者 Role.name
	
	/** module.name */
	private String module;           // Permission              或者Role     
	  
	private String path;             // role.id                 或者id
		
	private ModuleProp rootProp;     // role
		
	// 如果存在引用属性，则是外键引用的叶节点
	private ModuleProp refLeafProp;      // {id}                  或者 null
	
	// 如果存在引用属性，则是外键引用的叶节点
	private ModuleProp refLeafShowProp;  // {name}                或者 null
	
	// 可操作的查询方式，如果设置就默认为[{label: 'EQUAL', value: '='}]
	private List<Select> ops = new ArrayList<Select>();
	
	// 默认可选项
	private List<Select> values = new ArrayList<Select>();
		
	public FilterProp() {}
	
	public FilterProp(String module) {
		this.module = module;
	}
	
	public FilterProp clone() {
		FilterProp copy = new FilterProp();
		BeanUtils.copyProperties(this, copy);
		copy.setRootProp(rootProp.clone());
		
		if(refLeafProp != null) {
			copy.setRefLeafProp(refLeafProp.clone());
		}
		if(refLeafShowProp != null) {
			copy.setRefLeafProp(refLeafShowProp.clone());
		}
		
		List<Select> copyOfOps = new ArrayList<Select>();
		ops.forEach(op -> {
			copyOfOps.add(new Select(op.label, op.value));
		});
		copy.setOps(copyOfOps);
		
		List<Select> copyOfValues = new ArrayList<Select>();
		values.forEach(value -> {
			copyOfValues.add(new Select(value.label, value.value));
		});
		copy.setValues(copyOfValues);
		
		return this;
	}
	
	public String getOp() {
		if(ops.isEmpty()) {
			return Op.EQUAL.getViewOperator();
		}
		return ops.get(0).getValue();
	}
	
}
