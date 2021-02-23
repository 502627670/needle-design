package org.needleframe.core.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.needleframe.context.ModuleFactory;
import org.needleframe.core.exception.ServiceException;
import org.needleframe.core.model.Act;
import org.needleframe.core.model.FilterProp;
import org.needleframe.core.model.Module;
import org.needleframe.core.model.ModuleProp;
import org.needleframe.core.model.ModuleProp.Decoder;
import org.needleframe.core.model.ModuleProp.Encoder;
import org.needleframe.core.model.ModuleProp.Feature;
import org.needleframe.core.model.ModuleProp.RefModule;
import org.needleframe.core.model.ModuleProp.RuleType;
import org.needleframe.core.model.ModuleProp.Select;
import org.needleframe.core.model.ViewFilter.Op;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import lombok.Getter;

public class ModuleBuilder {
	
	private ModuleFactory mf;
	
	@Getter
	private Module module;
	
	public ModuleBuilder(Module module, ModuleFactory mf) {
		this.module = module;
		this.mf = mf;
	}
	
	public ModuleBuilder showName(String showName) {
		this.module.setShowName(showName);
		return this;
	}
	
	public ModuleBuilder showList(String... props) {
		List<String> showListProps = Arrays.asList(props);
		this.module.setShowListProps(showListProps);
		
		this.module.getProps().forEach((prop, mp) -> {
			if(!showListProps.contains(prop)) {
				mp.addRuleType(RuleType.HIDE_LIST);
			}
		});
		
		return this;
	}
	
	public ModuleBuilder securityGroup(String securityGroupProp) {
		this.module.setSecurityGroup(securityGroupProp);
		if(StringUtils.hasText(securityGroupProp)) {
			if(this.module.hasProp(securityGroupProp)) {
				this.module.getProp(securityGroupProp)
					.addRuleType(RuleType.SECURITY_GROUP)
					.addRuleType(RuleType.HIDE_LIST)
					.addRuleType(RuleType.HIDE_FORM)
					.addRuleType(RuleType.HIDE_EDIT)
					.addRuleType(RuleType.HIDE_INFO);
			}
		}
		return this;
	}
	
	public ModuleBuilder deletedProp(String deletedProp, Object deletedValue) {
		this.module.setDeletedProp(deletedProp);
		this.module.setDeletedValue(deletedValue);
		
		if(this.module.hasProp(deletedProp)) {
			this.module.getProp(deletedProp)
				.addRuleType(RuleType.HIDE_LIST)
				.addRuleType(RuleType.HIDE_INFO)
				.addRuleType(RuleType.HIDE_FORM)
				.addRuleType(RuleType.HIDE_EDIT);
		}
		return this;
	}
	
	/** 当指定为refSelf=true并且真实存在引用自身的关联属性，才会起作用 */
	public ModuleBuilder refSelf(boolean isRefSelf) {
		this.module.setRefSelf(isRefSelf);
		return this;
	}
	
	public ModuleFilterBuilder<? extends ModuleBuilder> filters(String... props) {
		return new ModuleFilterBuilder<ModuleBuilder>(this, props);
	}
	
	public ModuleBuilder noSecurityGroup() {
		this.module.setSecurityGroup(null);
		return this;
	}
	
	public ModuleBuilder publicData() {
		this.module.setPublicData(true);
		return this;
	}
	
	public ModuleBuilder enableTask(String taskProp) {
		String[] props = taskProp.split("\\.");
		if(props.length > 1) {
			throw new ServiceException("任务属性不允许嵌套，只允许普通属性");
		}
		this.module.setTaskProp(taskProp);
		return this;
	}
	
	public ModuleBuilder createdBy(String createdBy) {
		this.module.setCreatedBy(createdBy);
		return this;
	}
	
	public ModuleBuilder createdDate(String createdDate) {
		this.module.setCreatedDate(createdDate);
		return this;
	}
	
	public ModuleBuilder lastModifiedBy(String lastModifiedBy) {
		this.module.setLastModifiedBy(lastModifiedBy);
		return this;
	}
	
	public ModuleBuilder lastModifiedDate(String lastModifiedDate) {
		this.module.setLastModifiedDate(lastModifiedDate);
		return this;
	}
	
	public ModuleBuilder addUnique(String... props) {
		for(int i = 0; i < props.length; i++) {
			try {
				module.getProp(props[i]);
			}
			catch(Exception e) {
				throw new ServiceException("增加模块" + module.getName() + "唯一键失败：" + e.getMessage());
			}
		}
		this.module.getUniqueProps().add(props);
		return this;
	}
	
	public ModuleBuilder sort(Sort sort) {
		this.module.setSort(sort);
		return this;
	}
	
	public ModulePropBuilder<? extends ModuleBuilder> prop(String prop) {
		ModuleProp mp = module.getProp(prop);
		ModulePropBuilder<ModuleBuilder> mpBuilder = new ModulePropBuilder<ModuleBuilder>(mp, this);
		return mpBuilder;
	}
	
	public FkBuilder<? extends ModuleBuilder> fk(String prop) {
		ModuleProp mp = module.getProp(prop);
		ModulePropBuilder<ModuleBuilder> mpBuilder = new ModulePropBuilder<ModuleBuilder>(mp, this);
		return mpBuilder.fk();
	}
	
	public ModulePropBuilder<? extends ModuleBuilder> addProp(String prop, Class<?> type) {
		ModuleProp moduleProp = new ModuleProp();
		moduleProp.setModule(module);
		moduleProp.setName(prop);
		moduleProp.setProp(prop);
		moduleProp.setType(type);
		moduleProp.setColumn(prop);
		moduleProp.setSortOrder(module.getProps().size() + 1);
		this.module.getProps().put(prop, moduleProp);
		return new ModulePropBuilder<ModuleBuilder>(moduleProp, this);
	}
	
	public ChildModuleBuilder addChild(Class<?> childClass) {
		Module child = mf.build(childClass).getModule();
		List<ModuleProp> refPropList = child.getProps().values().stream().filter(mp -> {
			return mp.getType().isAssignableFrom(this.module.getModelClass());
		}).collect(Collectors.toList());
		if(refPropList.isEmpty()) {
			throw new ServiceException("子模块" + child.getName() + "与 父模块" + this.module.getName() + "没有关联，不能构建子模块");
		}
		refPropList.forEach(mp -> {
			String refProp = String.join(".", child.getName(), mp.getProp());
			this.module.getChildren().put(refProp, child);
		});
		return new ChildModuleBuilder(child, this).noAct();
	}
	
	public ChildModuleBuilder addChild(Class<?> childClass, String refProp) {
		Module child = mf.build(childClass).getModule();
		ModuleProp refModuleProp = child.getProp(refProp);
		if(refModuleProp == null) {
			throw new ServiceException("子模块" + child.getName() + "与 父模块" + this.module.getName() + "的关联属性" + refProp + "不存在");
		}
		refProp = String.join(".", child.getName(), refProp);
		this.module.getChildren().put(refProp, child);
		return new ChildModuleBuilder(child, this).noAct();
	}
	
	public ModuleFactory and() {
		return mf;
	}
	
	public ModuleBuilder addAct(Act act) {
		module.addAct(act);
		return this;
	}
	
	public ModuleBuilder addImport() {
		module.addAct(Act.IMPORT);
		return this;
	}
	
	public ModuleBuilder addExport() {
		module.addAct(Act.EXPORT);
		return this;
	}
	
	public ModuleBuilder addCRUD() {
		module.addAct(Act.CREATE);
		module.addAct(Act.UPDATE);
		module.addAct(Act.DELETE);
		return this;
	}
	
	public ModuleBuilder addIE() {
		module.addAct(Act.IMPORT);
		module.addAct(Act.EXPORT);
		return this;
	}
	
	public ModuleBuilder noCRUD() {
		module.noAct(Act.CREATE);
		module.noAct(Act.UPDATE);
		module.noAct(Act.DELETE);
		return this;
	}
	
	public ModuleBuilder noIE() {
		module.noAct(Act.IMPORT);
		module.noAct(Act.EXPORT);
		return this;
	}
	
	public ModuleBuilder act(Act act) {
		module.setActionMask(act.getMask());
		return this;
	}
	
	public ModuleBuilder noAct() {
		module.setActionMask(0);
		return this;
	}
	
	public ModuleBuilder noCreate() {
		module.noAct(Act.CREATE);
		return this;
	}
	
	public ModuleBuilder noUpdate() {
		module.noAct(Act.UPDATE);
		return this;
	}
	
	public ModuleBuilder noDelete() {
		module.noAct(Act.DELETE);
		return this;
	}
	
	public ModuleBuilder noImport() {
		module.noAct(Act.IMPORT);
		return this;
	}
	
	public ModuleBuilder noExport() {
		module.noAct(Act.EXPORT);
		return this;
	}
	
	public class ChildModuleBuilder extends ModuleBuilder {
		Module child;
		ModuleBuilder parent;
		
		public ChildModuleBuilder(Module child, ModuleBuilder parent) {
			super(child, mf);
			this.child = child;
			this.parent = parent;
		}
		
		public ChildModuleBuilder showList(String... props) {
			List<String> showListProps = Arrays.asList(props);
			this.child.setShowListProps(showListProps);
			
			this.child.getProps().forEach((prop, mp) -> {
				if(!showListProps.contains(prop)) {
					mp.addRuleType(RuleType.HIDE_LIST);
				}
			});
			
			return this;
		}
		
		public ModulePropBuilder<ChildModuleBuilder> prop(String prop) {
			ModuleProp mp = child.getProp(prop);
			ModulePropBuilder<ChildModuleBuilder> mpBuilder = 
					new ModulePropBuilder<ChildModuleBuilder>(mp, this);
			return mpBuilder;
		}
		
		public FkBuilder<ChildModuleBuilder> fk(String prop) {
			ModuleProp mp = child.getProp(prop);
			return new ModulePropBuilder<ChildModuleBuilder>(mp, this).fk();
		}
		
		public ModulePropBuilder<ChildModuleBuilder> addProp(String prop, Class<?> type) {
			ModuleProp moduleProp = new ModuleProp();
			moduleProp.setModule(child);
			moduleProp.setName(prop);
			moduleProp.setProp(prop);
			moduleProp.setType(type);
			moduleProp.setColumn(prop);
			moduleProp.setSortOrder(child.getProps().size() + 1);
			this.child.getProps().put(prop, moduleProp);
			return new ModulePropBuilder<ChildModuleBuilder>(moduleProp, this);
		}
			
		public ModuleFilterBuilder<ChildModuleBuilder> filters(String... props) {
			return new ModuleFilterBuilder<ChildModuleBuilder>(this, props);
		}
		
		public ChildModuleBuilder deletedProp(String deletedProp, Object deletedValue) {
			this.child.setDeletedProp(deletedProp);
			this.child.setDeletedValue(deletedValue);
			
			if(this.child.hasProp(deletedProp)) {
				this.child.getProp(deletedProp)
					.addRuleType(RuleType.HIDE_LIST)
					.addRuleType(RuleType.HIDE_INFO)
					.addRuleType(RuleType.HIDE_FORM)
					.addRuleType(RuleType.HIDE_EDIT);
			}
			return this;
		}
		
		public ChildModuleBuilder showName(String showName) {
			this.child.setShowName(showName);
			return this;
		}
		
		public ChildModuleBuilder securityGroupProp(String securityGroup) {
			this.child.setSecurityGroup(securityGroup);
			return this;
		}
		
		public ChildModuleBuilder noSecurityGroup() {
			this.child.setSecurityGroup(null);
			return this;
		}
		
		public ChildModuleBuilder publicData() {
			this.child.setPublicData(true);
			return this;
		}
		
		public ChildModuleBuilder enableTask(String taskProp) {
			this.child.setTaskProp(taskProp);
			return this;
		}
		
		public ChildModuleBuilder sort(Sort sort) {
			this.child.setSort(sort);
			return this;
		}
		
		public ChildModuleBuilder addAct(Act act) {
			this.child.addAct(act);
			return this;
		}
		
		public ChildModuleBuilder addImport() {
			this.child.addAct(Act.IMPORT);
			return this;
		}
		
		public ChildModuleBuilder addExport() {
			this.child.addAct(Act.EXPORT);
			return this;
		}
		
		@Override
		public ChildModuleBuilder addCRUD() {
			super.addCRUD();
			return this;
		}
		
		public ChildModuleBuilder addIE() {
			super.addIE();
			return this;
		}
		
		public ChildModuleBuilder act(Act act) {
			child.setActionMask(act.getMask());
			return this;
		}
		
		public ChildModuleBuilder noAct() {
			super.noAct();
			return this;
		}
		@Override
		public ChildModuleBuilder noCreate() {
			super.noCreate();
			return this;
		}
		@Override
		public ChildModuleBuilder noUpdate() {
			super.noUpdate();
			return this;
		}
		@Override
		public ChildModuleBuilder noDelete() {
			super.noDelete();
			return this;
		}
		@Override
		public ChildModuleBuilder noImport() {
			super.noImport();
			return this;
		}
		@Override
		public ChildModuleBuilder noExport() {
			super.noExport();
			return this;
		}
		
		public ChildModuleBuilder addChild(Class<?> childClass) {
			Module subchild = mf.build(childClass).getModule();
			List<ModuleProp> refPropList = subchild.getProps().values().stream().filter(mp -> {
				return mp.getType().isAssignableFrom(this.child.getModelClass());
			}).collect(Collectors.toList());
			if(refPropList.isEmpty()) {
				throw new ServiceException("子模块" + subchild.getName() + "与 父模块" + this.child.getName() + "没有关联，不能构建子模块");
			}
			refPropList.forEach(mp -> {
				String refProp = String.join(".", subchild.getName(), mp.getProp());
				this.child.getChildren().put(refProp, subchild);
			});
			return new ChildModuleBuilder(subchild, this).noAct();
		}
		
		public ChildModuleBuilder addChild(Class<?> childClass, String refProp) {
			Module subchild = mf.build(childClass).getModule();
			ModuleProp refModuleProp = subchild.getProp(refProp);
			if(refModuleProp == null) {
				throw new ServiceException("子模块" + subchild.getName() + "与 父模块" + 
						this.child.getName() + "的关联属性" + refProp + "不存在");
			}
			refProp = String.join(".", subchild.getName(), refProp);
			this.child.getChildren().put(refProp, subchild);
			return new ChildModuleBuilder(subchild, this).noAct();
		}
		
		public ModuleBuilder endChild() {
			return this.parent;
		}
	}
	
	@Getter
	public class ModulePropBuilder<T extends ModuleBuilder> {
		ModuleProp mp;
		Module module;
		T builder;
		
		public ModulePropBuilder(ModuleProp mp, T builder) {
			this.mp = mp;
			this.module = mp.getModule();
			this.builder = builder;
		}
		public ModulePropBuilder<T> name(String name) {
			mp.name(name);
			return this;
		}
		public ModulePropBuilder<T> type(Class<?> type) {
			mp.type(type);
			if(String.class.isAssignableFrom(type)) {
				mp.feature(Feature.STRING);
			}
			return this;
		}
		
		public ModulePropBuilder<T> column(String column) {
			mp.column(column);
			return this;
		}
		
		public ModulePropBuilder<T> length(int length) {
			mp.length(length);
			return this;
		}
		
		public ModulePropBuilder<T> pattern(String pattern) {
			mp.pattern(pattern);
			return this;
		}
		
		public ModulePropBuilder<T> sortOrder(int sortOrder) {
			mp.sortOrder(sortOrder);
			return this;
		}
		
		public ModulePropBuilder<T> defaultValue(Object defaultValue) {
			mp.defaultValue(defaultValue);
			return this;
		}
		
		public ModulePropBuilder<T> transientProp(boolean transientProp) {
			mp.transientProp(transientProp);
			return this;
		}
		
		public ModulePropBuilder<T> securityGroup(boolean securityGroup) {
			mp.securityGroup(securityGroup);
			return this;
		}
		
		public ModulePropBuilder<T> feature(Feature feature) {
			mp.feature(feature);
			mp.addRuleType(RuleType.SHOW_IN_ROW);
			return this;
		}
		
		public ModulePropBuilder<T> values(Object[] values) {
			List<Select> selectValues = new ArrayList<Select>();
			for(int i = 0; i < values.length; i++) {
				String value = values[i].toString();
				selectValues.add(new Select(value, value));
			}
			mp.values(selectValues);
			mp.feature(Feature.SELECT);
			return this;
		}
		
		public ModulePropBuilder<T> values(List<Select> values) {
			mp.values(values);
			mp.feature(Feature.SELECT);
			return this;
		}
		
		public ModulePropBuilder<T> encoder(Encoder encoder) {
			mp.setEncoder(encoder);
			return this;
		}
		
		public ModulePropBuilder<T> decoder(Decoder decoder) {
			mp.setDecoder(decoder);
			return this;
		}
		
		public ModulePropBuilder<T> date() {
			mp.pattern("yyyy-MM-dd");
			mp.setFeature(Feature.DATE);
			return this;
		}
		
		public ModulePropBuilder<T> dateTime() {
			mp.pattern("yyyy-MM-dd HH:mm:ss");
			mp.setFeature(Feature.DATETIME);
			return this;
		}
		
		public ModulePropBuilder<T> text() {
			int length = mp.getLength() == null ? 255 : mp.getLength();
			mp.length(length);
			mp.feature(Feature.TEXT);
			mp.addRuleType(RuleType.HIDE_LIST);
			this.inRow();
			return this;
		}
		
		public ModulePropBuilder<T> absoluteFile() {
			this.file();
			mp.addRuleType(RuleType.ABSOLUTE_FILE);
			return this;
		}
		
		public ModulePropBuilder<T> file() {
			mp.feature(Feature.FILE);
			mp.addRuleType(RuleType.HIDE_LIST);
			this.inRow();
			return this;
		}
		
		public ModulePropBuilder<T> absoluteImage() {
			this.image();
			mp.addRuleType(RuleType.ABSOLUTE_FILE);
			return this;
		}
		
		public ModulePropBuilder<T> image() {
			mp.feature(Feature.IMAGE);
			mp.addRuleType(RuleType.HIDE_LIST);
			this.inRow();
			return this;
		}
		
		public ModulePropBuilder<T> editor() {
			mp.feature(Feature.EDITOR);
			mp.addRuleType(RuleType.HIDE_LIST);
			this.inRow();
			return this;
		}
		
		public ModulePropBuilder<T> inRow() {
			mp.addRuleType(RuleType.SHOW_IN_ROW);
			return this;
		}
		
		public ModulePropBuilder<T> inCol() {
			int ruleType = mp.getRuleType();
			int mask = RuleType.SHOW_IN_ROW.getMask();
			if((mask & ruleType) == mask) {
				ruleType ^= mask;
			}
			mp.setRuleType(ruleType);
			return this;
		}
		
		public ModulePropBuilder<T> show() {
			this.showList().showInfo().showForm().showEdit();
			return this;
		}
		
		public ModulePropBuilder<T> showInfo() {
			int ruleType = mp.getRuleType();
			int mask = RuleType.HIDE_INFO.getMask();
			if((mask & ruleType) == mask) {
				ruleType ^= mask;
			}
			mp.setRuleType(ruleType);
			return this;
		}
		
		public ModulePropBuilder<T> showList() {
			int ruleType = mp.getRuleType();
			int mask = RuleType.HIDE_LIST.getMask();
			if((mask & ruleType) == mask) {
				ruleType ^= mask;
			}
			mp.setRuleType(ruleType);
			return this;
		}
		
		public ModulePropBuilder<T> showForm() {
			int ruleType = mp.getRuleType();
			int mask = RuleType.HIDE_FORM.getMask();
			if((mask & ruleType) == mask) {
				ruleType ^= mask;
			}
			mp.setRuleType(ruleType);
			return this;
		}
		
		public ModulePropBuilder<T> showEdit() {
			int ruleType = mp.getRuleType();
			int mask = RuleType.HIDE_EDIT.getMask();
			if((mask & ruleType) == mask) {
				ruleType ^= mask;
			}
			mp.setRuleType(ruleType);
			return this;
		}
		
		public ModulePropBuilder<T> hideList() {
			mp.addRuleType(RuleType.HIDE_LIST);
			return this;
		}
		public ModulePropBuilder<T> hideForm() {
			mp.addRuleType(RuleType.HIDE_FORM);
			return this;
		}
		public ModulePropBuilder<T> hideEdit() {
			mp.addRuleType(RuleType.HIDE_EDIT);
			return this;
		}
		public ModulePropBuilder<T> hideInfo() {
			mp.addRuleType(RuleType.HIDE_INFO);
			return this;
		}
		
		public ModulePropBuilder<T> hide() {
			this.hideInfo().hideForm().hideEdit().hideList();
			return this;
		}
		
		public ModulePropBuilder<T> system() {
			this.hide();
			this.mp.addRuleType(RuleType.SYSTEM_FIELD);
			return this;
		}
		
		public ModulePropBuilder<T> required() {
			mp.addRuleType(RuleType.REQUIRED);
			return this;
		}
		
		
		
		/** 指定外键，最多支持2级嵌套，比如x.y属性 */
		public FkBuilder<T> fk() {
			return new FkBuilder<T>(this, mp.getProp());
		}
		
		public RuleBuilder<T> rule() {
			return new RuleBuilder<T>(this, mp.getProp());
		}
		
		public ModulePropBuilder<T> rule(RuleType... rules) {
			RuleBuilder<T> ruleBuilder = new RuleBuilder<T>(this, mp.getProp());
			for(int i = 0; i < rules.length; i++) {
				ruleBuilder.addRule(rules[i]);
			}
			return this;
		}
		
		/** 可搜索属性，支持嵌套格式*/
		public FilterBuilder<T> filter() {
			return new FilterBuilder<T>(this, mp.getProp());
		}
		
		public ModulePropBuilder<T> filter(Op... ops) {
			return new FilterBuilder<T>(this, mp.getProp()).ops(ops);
		}
		
		public T end() {
			return this.builder;
		}
		
		public ModulePropBuilder<T> prop(String prop) {
			ModuleProp mp = module.getProp(prop);
			ModulePropBuilder<T> mpBuilder = new ModulePropBuilder<T>(mp, builder);
			return mpBuilder;
		}
		
		public ModuleFactory and() {
			return ModuleBuilder.this.mf;
		}
	}
	
	public class FkBuilder<T extends ModuleBuilder> {
		private ModuleProp moduleProp;
		private ModulePropBuilder<T> builder;
		private FkBuilder(ModulePropBuilder<T> builder, String prop) {
			this.builder = builder;
			Module module = this.builder.getModule();
			this.moduleProp = this.builder.getMp();
			
			String[] propArray = prop.split("\\.");
			if(propArray.length > 2) {
				throw new ServiceException("模块" + module.getName() + "的外键属性" + prop + "超过了2级（只允许x或x.y）");
			}
			ModuleProp moduleProp = module.getProp(propArray[0]);
			RefModule refModule = moduleProp.getRefModule();
			if(refModule == null) {
				refModule = new RefModule();
				moduleProp.setRefModule(refModule);
			}
			if(propArray.length == 2) {
				ref(propArray[1]);
			}
		}
		/** 级联删除：父记录删除时，同步删除子记录 */
		public FkBuilder<T> cascadeDelete() {
			RefModule refModule = this.moduleProp.getRefModule();
			if(refModule == null) {
				refModule = new RefModule();
				this.moduleProp.setRefModule(refModule);
			}
			refModule.setCascadeDel(true);
			return this;
		}
		public FkBuilder<T> maps(String... refProps) {
			for(int i = 0; i < refProps.length; i++) {
				map(refProps[i], refProps[i]);
			}
			return this;
		}
		public FkBuilder<T> map(String refProp, String prop) {
			this.moduleProp.getMappings().put(refProp, prop);
			return this;
		}
		public FkBuilder<T> ref(String refProp) {
			RefModule refModule = this.moduleProp.getRefModule();
			refModule.setRefProp(refProp);
			refModule.setRefPropType(moduleProp.getType());
			return this;
		}
		public ModulePropBuilder<T> show(String showProp) {
			RefModule refModule = this.moduleProp.getRefModule();
			refModule.setRefShowProp(showProp);
			if(refModule.getRefShowProp().equals(refModule.getRefProp())) {
				refModule.setRefProp(null);
			}
			return builder;
		}
	}
	
	public class ModuleFilterBuilder<T extends ModuleBuilder> {
		T builder;
		String[] props;
		List<Select> values = new ArrayList<Select>();
		private ModuleFilterBuilder(T builder, String... props) {
			this.builder = builder;
			this.props = props;
		}
		public T eq() {
			return op(Op.EQUAL);
		}
		public ModuleFilterBuilder<T> addOption(String value) {
			this.values.add(new Select(value, value));
			return this;
		}
		public ModuleFilterBuilder<T> addOption(String label, String value) {
			this.values.add(new Select(label, value));
			return this;
		}
		public ModuleFilterBuilder<T> options(Map<String,String> options) {
			options.forEach((label, value) -> {
				this.values.add(new Select(label, value));
			});
			return this;
		}
		public T op(Op op) {
			Module module = builder.getModule();
			List<Select> ops = Arrays.asList(new Select(op.getViewOperator(), op.getOperator()));
			for(int i = 0; i < props.length; i++) {
				FilterProp fp = new FilterProp(module.getName());
				fp.setPath(props[i]);
				fp.setOps(ops);
				fp.setValues(values);
				module.addFilterProp(fp);
			}
			return builder;
		}
		public T ops(Op... ops) {
			Module module = builder.getModule();
			List<Select> opList = new ArrayList<Select>();
			for(int i = 0; i < ops.length; i++) {
				opList.add(new Select(ops[i].getViewOperator(), ops[i].getOperator()));
			}
			for(int i = 0; i < props.length; i++) {
				FilterProp fp = new FilterProp(module.getName());
				fp.setPath(props[i]);
				fp.setOps(opList);
				fp.setValues(values);
				module.addFilterProp(fp);
			}
			return builder;
		}
	}
	
	public class FilterBuilder<T extends ModuleBuilder> {
		ModulePropBuilder<T> builder;
		String[] props;
		List<Select> values = new ArrayList<Select>();
		private FilterBuilder(ModulePropBuilder<T> builder, String... props) {
			this.builder = builder;
			this.props = props;
		}
		public ModulePropBuilder<T> eq() {			
			return op(Op.EQUAL);
		}
		public FilterBuilder<T> addOption(String value) {
			this.values.add(new Select(value, value));
			return this;
		}
		public FilterBuilder<T> addOption(String label, String value) {
			this.values.add(new Select(label, value));
			return this;
		}
		public FilterBuilder<T> options(Map<String,String> options) {
			options.forEach((label, value) -> {
				this.values.add(new Select(label, value));
			});
			return this;
		}
		public ModulePropBuilder<T> op(Op op) {
			Module module = builder.getModule();
			List<Select> ops = Arrays.asList(new Select(op.getViewOperator(), op.getOperator()));
			for(int i = 0; i < props.length; i++) {
				FilterProp fp = new FilterProp(module.getName());
				fp.setPath(props[i]);
				fp.setOps(ops);
				fp.setValues(values);
				module.addFilterProp(fp);
			}
			return builder;
		}
		public ModulePropBuilder<T> ops(Op... ops) {
			Module module = builder.getModule();
			List<Select> opList = new ArrayList<Select>();
			for(int i = 0; i < ops.length; i++) {
				opList.add(new Select(ops[i].getViewOperator(), ops[i].getOperator()));
			}
			for(int i = 0; i < props.length; i++) {
				FilterProp fp = new FilterProp(module.getName());
				fp.setPath(props[i]);
				fp.setOps(opList);
				fp.setValues(values);
				module.addFilterProp(fp);
			}
			return builder;
		}
	}
	
	public class RuleBuilder<T extends ModuleBuilder> {
		private ModulePropBuilder<T> builder;
		private List<ModuleProp> moduleProps = new ArrayList<ModuleProp>();
		private RuleBuilder(ModulePropBuilder<T> builder, String... props) {
			Module module = builder.getModule();
			this.builder = builder;
			for(int i = 0; i < props.length; i++) {
				this.moduleProps.add(module.getProp(props[i]));
			}
		}
		public RuleBuilder<T> addRule(RuleType ruleType) {
			moduleProps.forEach(mp -> {
				mp.addRuleType(ruleType);
			});
			return this;
		}
		public ModulePropBuilder<T> addEnd(RuleType ruleType) {
			this.addRule(ruleType);
			return builder;
		}
		public ModulePropBuilder<T> required() {
			this.addRule(RuleType.REQUIRED);
			return builder;
		}
		public ModulePropBuilder<T> noCreate() {
			this.addRule(RuleType.NO_CREATE);
			return builder;
		}
		public ModulePropBuilder<T> noUpdate() {
			this.addRule(RuleType.NO_UPDATE);
			return builder;
		}
		public ModulePropBuilder<T> end() {
			return builder;
		}
		public ModulePropBuilder<T> rule(RuleType ruleType) {
			moduleProps.forEach(mp -> mp.addRuleType(ruleType));
			return builder;
		}
	}
	
}
