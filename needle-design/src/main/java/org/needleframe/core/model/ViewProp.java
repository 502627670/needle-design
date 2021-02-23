package org.needleframe.core.model;

import java.util.ArrayList;
import java.util.List;

import org.needleframe.core.model.ModuleProp.Decoder;
import org.needleframe.core.model.ModuleProp.Encoder;
import org.needleframe.core.model.ModuleProp.Feature;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ViewProp {
	
	// 名称
	private String name;
	
	/**
	 * 支持普通属性x，或者级联属性y.b.c
	 */
	private String prop;
	
	/**
	 * prop的类型，Service方法会计算并设置该值
	 */
	private Class<?> type;
	
	// 格式
	private String pattern;
	
	private int ruleType;
	
	private Feature feature;
	
	private Encoder encoder;
	
	private Decoder decoder;
	
	public ViewProp() {}
	
	public ViewProp(String prop) {
		this.prop = prop;
		this.name = this.prop.split("\\.")[0];
	}
	
	public static List<ViewProp> from(Module module) {
		List<ViewProp> viewProps = new ArrayList<ViewProp>();
		module.getProps().values().stream().filter(mp -> !mp.isTransientProp()).forEach(mp -> {
			ViewProp vp = new ViewProp(mp.getProp());
			viewProps.add(vp);
		});
		return viewProps;
	}
	
	public static List<ViewProp> from(String... props) {
		List<ViewProp> viewProps = new ArrayList<ViewProp>();
		for(int i = 0; i < props.length; i++) {
			ViewProp vp = new ViewProp(props[i]);
			viewProps.add(vp);
		}
		return viewProps;
	}
	
}
