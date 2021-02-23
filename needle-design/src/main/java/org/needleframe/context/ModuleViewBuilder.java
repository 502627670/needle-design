package org.needleframe.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.needleframe.core.jdbc.QueryFilter;
import org.needleframe.core.jdbc.QueryFilter.QuerySubFilter;
import org.needleframe.core.jdbc.QueryProp;
import org.needleframe.core.jdbc.QuerySort;
import org.needleframe.core.model.Module;
import org.needleframe.core.model.ModuleProp;
import org.needleframe.core.model.ModuleProp.RefModule;
import org.needleframe.core.model.ViewFilter;
import org.needleframe.core.model.ViewFilter.Op;
import org.needleframe.core.model.ViewFilter.SubQuery;
import org.needleframe.core.model.ViewJoin;
import org.needleframe.core.model.ViewProp;
import org.needleframe.utils.BeanUtils;
import org.needleframe.utils.DateUtils;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

public class ModuleViewBuilder {
	
	private ModuleContext moduleContext;
	
	public ModuleViewBuilder(ModuleContext moduleContext) {
		this.moduleContext = moduleContext;
	}
	
	public List<QueryProp> buildProps(Module module, List<ViewProp> viewProps) {
		return viewProps.stream()
			.filter(vp -> !module.getProp(vp.getProp().split("\\.")[0]).isTransientProp())
			.map(vp -> buildProp(module, vp, false))
			.collect(Collectors.toList());
	}
	
	public List<QueryFilter> buildFilters(Module module, List<? extends ViewFilter> viewFilters) {
		return viewFilters.stream()
			.filter(vf -> {
				if(module.getProp(vf.getProp().split("\\.")[0]).isTransientProp()) {
					return false;
				}
				Object value = vf.getValue();
				if(value == null || !StringUtils.hasText(value.toString())) {
					if(Op.IS_NULL.match(vf.getOp())) {
						return true;
					}
					return false;
				}
				if(Collection.class.isAssignableFrom(value.getClass())) {
					Collection<?> valueList = (Collection<?>) value;
					if(valueList.isEmpty()) {
						return false;
					}
				}
				return true;
			})
			.flatMap(vf -> buildFilter(module, vf).stream())
//			.map(vf -> buildFilter(module, vf))
			.collect(Collectors.toList());
	}
	
	/**
	 * 构建嵌套属性的引用关系
	 * @param module
	 * @param viewProp  可以是嵌套属性，比如userRoles.role.user.name 或者 userRoles.role.saleId.Sale.name
	 * @return
	 */
	public QueryProp buildProp(Module module, ViewProp viewProp, boolean isSubProp) {
		String originalProp = viewProp.getProp();
		String prop = originalProp;
		String[] propArray = prop.split("\\.");
		ModuleProp moduleProp = module.getProp(propArray[0]);
		RefModule refModule = moduleProp.getRefModule();
		if(propArray.length == 1 && moduleProp.isRefParent()) {
			prop += "." + (isSubProp ? refModule.getRefProp() : refModule.getRefShowProp());
			propArray = prop.split("\\.");
		}
		QueryProp queryProp = new QueryProp(viewProp);
		if(propArray.length == 2) {
			String name = moduleProp.getName();
			if(module.hasProp(prop)) {
				name = module.getProp(prop).getName();
			} 
			if(refModule == null) {
				queryProp.setLastColumn(moduleProp.getColumn());
				queryProp.setShowColumn(moduleProp.getColumn());
				queryProp.setShowPath(viewProp.getProp());
				viewProp.setType(moduleProp.getType());
				viewProp.setPattern(moduleProp.getPattern());
				viewProp.setName(name);
				viewProp.setRuleType(moduleProp.getRuleType());
				viewProp.setFeature(moduleProp.getFeature());
				viewProp.setEncoder(moduleProp.getEncoder());
				viewProp.setDecoder(moduleProp.getDecoder());
				return queryProp;
			}
			else {
				Module next = moduleContext.getModule(refModule.getRefModuleName(), true);
				if(next.getPk().equals(propArray[1])) {
					queryProp.setLastColumn(moduleProp.getColumn());
					queryProp.setShowColumn(moduleProp.getColumn());
					queryProp.setShowPath(viewProp.getProp());
					viewProp.setType(moduleProp.getType());
					viewProp.setPattern(moduleProp.getPattern());
					viewProp.setName(name);
					viewProp.setRuleType(moduleProp.getRuleType());
					viewProp.setFeature(moduleProp.getFeature());
					viewProp.setEncoder(moduleProp.getEncoder());
					viewProp.setDecoder(moduleProp.getDecoder());
					return queryProp;
				}
			}
		}
		viewProp.setRuleType(moduleProp.getRuleType());
		if(module.hasProp(originalProp)) {
			viewProp.setRuleType(module.getProp(originalProp).getRuleType());
		}
		else if(module.hasProp(prop)) {
			viewProp.setRuleType(module.getProp(prop).getRuleType());
		}
		return buildPropRefs(module, viewProp);
	}
	
	/**
	 * 构建嵌套属性的引用关系
	 * @param module
	 * @param viewFilter  可以是嵌套属性，比如userRoles.role.user.name 或者 userRoles.role.saleId.Sale.name
	 * @return
	 */
	public List<QueryFilter> buildFilter(Module module, ViewFilter viewFilter) {
		String prop = viewFilter.getProp();
		String[] propArray = prop.split("\\.");
		ModuleProp moduleProp = module.getProp(propArray[0]);
		RefModule firstRefModule = moduleProp.getRefModule();
		if(propArray.length == 1 && moduleProp.isRefParent()) {
			prop += "." + firstRefModule.getRefProp();
			propArray = prop.split("\\.");
		}
		
		QueryFilter queryFilter = new QueryFilter(viewFilter);
		if(propArray.length == 2) {
			if(firstRefModule == null) {
				queryFilter.setLastColumn(moduleProp.getColumn());
				Object value = resolveFilterValue(viewFilter, moduleProp);
				queryFilter.value(value);
				return resolveQueryFilter(queryFilter);
			}
			else {
				Module next = moduleContext.getModule(firstRefModule.getRefModuleName(), true);
				if(next.getPk().equals(propArray[1])) {
					queryFilter.setLastColumn(moduleProp.getColumn());
					Object value = resolveFilterValue(viewFilter, moduleProp);
					queryFilter.value(value);
					return resolveQueryFilter(queryFilter);
				}
			}
		}
		
		List<ViewJoin> joins = new ArrayList<ViewJoin>();
		ModuleProp last = buildRefs(module, viewFilter.getProp(), joins);
		queryFilter.setViewJoins(joins);
		queryFilter.setLastColumn(last.getColumn());
		
		ModuleProp mp = last;
		RefModule refModule = last.getRefModule();
		if(refModule != null) {
			String refModuleName = refModule.getRefModuleName();
			Module ref = moduleContext.getModule(refModuleName);
			ModuleProp next = ref.getProp(ref.getPk());
			mp = next;
		}
		
		Object value = resolveFilterValue(viewFilter, mp);
		queryFilter.value(value);
		return resolveQueryFilter(queryFilter);
	}
	
	private List<QueryFilter> resolveQueryFilter(QueryFilter queryFilter) {
		Object value = queryFilter.getValue();
		if(value != null && Date.class.isAssignableFrom(value.getClass())) {
			ViewFilter viewFilter = queryFilter.getViewFilter();
			Date next = DateUtils.getDateNext(new Date(((Date) value).getTime()), 1);
			if(Op.EQUAL.match(viewFilter.getOp())) {
				List<QueryFilter> filters = new ArrayList<QueryFilter>();
				
				queryFilter.setViewFilter(ViewFilter.ge(viewFilter.getProp(), value));
				
				QueryFilter clone = queryFilter.clone();
				clone.setViewFilter(ViewFilter.lt(viewFilter.getProp(), next));
				clone.setValue(next);
				
				filters.add(queryFilter);
				filters.add(clone);
				return filters;
			}
		}
		return Arrays.asList(queryFilter);
	}
	
	private Object resolveFilterValue(ViewFilter viewFilter, ModuleProp mp) {
		Object value = viewFilter.getValue();
		if(value != null) {
			Class<?> type = mp.getType();
			if(value instanceof SubQuery) {
				SubQuery subQuery = ((SubQuery) value);
				Module childModule = moduleContext.getModule(subQuery.getModule(), true);
				QueryProp queryProp = buildProp(childModule, subQuery.getViewProp(), true);
				List<QueryFilter> queryFilters = buildFilters(childModule, subQuery.getFilters());
				value = new QuerySubFilter(childModule, queryProp, queryFilters);
			}
			else if(Collection.class.isAssignableFrom(value.getClass())) {
				Collection<?> valueList = (Collection<?>) value;
				List<Object> values = new ArrayList<Object>(valueList.size());
				Class<?> valueType = type;
				valueList.forEach(v -> {
					values.add(BeanUtils.convert(valueType, v.toString()));
				});
				value = values;
			}
			else {
				if(mp.getEncoder() != null) {
					value = mp.getEncoder().encode(value);
				}
				
				if(Enum.class.isAssignableFrom(type)) {
					value = value.toString();
				}
				else {
					Class<?> clazz = org.springframework.util.ClassUtils.getUserClass(value);
					if(org.springframework.beans.BeanUtils.isSimpleProperty(clazz)) {
						value = BeanUtils.convert(type, value.toString());
					}
					else {
						Module valueModule = moduleContext.getModule(clazz, false);
						value = valueModule.getBeanWrapper().getPropertyValue(valueModule.getPk());
					}
				}
			}
		}
		return value;
	}
	
	public List<QuerySort> buildSort(Module module, Sort sort) {
		List<QuerySort> querySortList = new ArrayList<QuerySort>();
		sort.forEach(order -> {
			String prop = order.getProperty();
			List<ViewJoin> joins = new ArrayList<ViewJoin>();
			ModuleProp last = buildRefs(module, prop, joins);
			
			QuerySort querySort = new QuerySort(order);
			querySort.setLastColumn(last.getColumn());
			querySort.setViewJoins(joins);
			querySortList.add(querySort);
		});
		return querySortList;
	}
	
	private QueryProp buildPropRefs(Module module, ViewProp viewProp) {
		String[] propArray = viewProp.getProp().split("\\.");
		
		Module prev = module;
		ModuleProp last = null, show = null;
		String path = viewProp.getProp();
		List<ViewJoin> joins = new ArrayList<ViewJoin>();
		for(int i = 0; i < propArray.length; i++) {
			String propName = propArray[i];
			String firstLetter = propName.substring(0, 1);
			if(firstLetter.toUpperCase().equals(firstLetter)) {
				String prevProp = propArray[i - 1];  // userRoles.role.saleId.Sale.user
				ModuleProp nextModuleProp = prev.getProp(prevProp);
				String refModuleName = firstLetter.toLowerCase() + propName.substring(1);
				Module ref = moduleContext.getModule(refModuleName, true);
				path = String.join(".", Arrays.copyOfRange(propArray, 0, i + 1));
				ViewJoin viewJoin = new ViewJoin();
				viewJoin.setPath(path);
				viewJoin.setTable(prev.getTable());
				viewJoin.setColumn(nextModuleProp.getColumn());
				viewJoin.setRefTable(ref.getTable());
				viewJoin.setRefColumn(ref.getProp(ref.getPk()).getColumn());
				joins.add(viewJoin);
				
				prev = ref;
				last = nextModuleProp;
				show = last;
				if(i == propArray.length - 1) {  // 处理merchant_id.Merchant这种情况
					RefModule nextRefModule = nextModuleProp.getRefModule();
					if(nextRefModule != null) {
						last = ref.getProp(nextRefModule.getRefProp());
						show = ref.getProp(nextRefModule.getRefShowProp());
					}
					else {
						last = ref.getProp(ref.getPk());
						show = last;
					}
				}
			}
			else {
				last = prev.getProp(propName);
				show = last;
				RefModule lastRefModule = last.getRefModule();
				if(lastRefModule != null) {
					path = String.join(".", Arrays.copyOfRange(propArray, 0, i + 1));
					Module ref = moduleContext.getModule(lastRefModule.getRefModuleName(), true);
					ViewJoin viewJoin = new ViewJoin();
					viewJoin.setPath(path);
					viewJoin.setTable(prev.getTable());
					viewJoin.setColumn(prev.getProp(lastRefModule.getProp()).getColumn());
					viewJoin.setRefTable(ref.getTable());
					viewJoin.setRefColumn(ref.getProp(lastRefModule.getRefProp()).getColumn());
					joins.add(viewJoin);
					prev = ref;
					if(i == propArray.length - 1) {
						last = ref.getProp(lastRefModule.getRefProp());
						show = ref.getProp(lastRefModule.getRefShowProp());
					}
				}
			}
		}
		QueryProp queryProp = new QueryProp(viewProp);
		queryProp.setLastColumn(last.getColumn());
		queryProp.setShowColumn(show.getColumn());
		queryProp.setViewJoins(joins);
		viewProp.setType(show.getType());
		viewProp.setPattern(show.getPattern());
		
		ModuleProp firstMp = module.getProp(propArray[0]);
		viewProp.setName(firstMp.getName());
		if(module.hasProp(viewProp.getProp())) {
			viewProp.setName(module.getProp(viewProp.getProp()).getName());
		}
		
		viewProp.setFeature(show.getFeature());
		viewProp.setEncoder(show.getEncoder());
		viewProp.setDecoder(show.getDecoder());
		if(!last.equals(show)) {
			queryProp.setShowPath(String.join(".", path, show.getProp()));
			viewProp.setProp(queryProp.getShowPath());
		}
		return queryProp;
	}
		
	/**
	 * @param module
	 * @param prop  可以是嵌套属性，比如userRoles.role.user.name 或者 userRoles.role.saleId.Sale.name
	 * @param refModuleList
	 * @return
	 */
	private ModuleProp buildRefs(Module module, String prop, List<ViewJoin> joins) {
		String[] propArray = prop.split("\\.");
		Module prev = module;
		ModuleProp last = prev.getProp(propArray[0]);
		
		// group, group.id, group.name
		if(propArray.length == 1) {
			return last;
		}
		else if(propArray.length == 2) {
			RefModule lastRefModule = last.getRefModule();
			if(lastRefModule != null) {
				Module next = moduleContext.getModule(lastRefModule.getRefModuleName(), true);
				if(next.getPk().equals(propArray[1])) {
					return last;
				}
			}
		}
		
		for(int i = 0; i < propArray.length; i++) {
			String propName = propArray[i];
			String firstLetter = propName.substring(0, 1);
			if(firstLetter.toUpperCase().equals(firstLetter)) {
				String prevProp = propArray[i - 1];  // userRoles.role.saleId.Sale.user
				ModuleProp nextModuleProp = prev.getProp(prevProp);
				String refModuleName = firstLetter.toLowerCase() + propName.substring(1);
				Module ref = moduleContext.getModule(refModuleName, true);
				
				ViewJoin viewJoin = new ViewJoin();
				viewJoin.setPath(String.join(".", Arrays.copyOfRange(propArray, 0, i + 1)));
				viewJoin.setTable(prev.getTable());
				viewJoin.setColumn(nextModuleProp.getColumn());
				viewJoin.setRefTable(ref.getTable());
				viewJoin.setRefColumn(ref.getProp(ref.getPk()).getColumn());
				joins.add(viewJoin);
				
				prev = ref;
				last = nextModuleProp;
				if(i == propArray.length - 1) {  // 处理merchant_id.Merchant这种情况
					last = ref.getProp(ref.getPk());
				}
			}
			else {
				last = prev.getProp(propName);
				RefModule lastRefModule = last.getRefModule();
				if(lastRefModule != null) {
					Module ref = moduleContext.getModule(lastRefModule.getRefModuleName(), true);
					ViewJoin viewJoin = new ViewJoin();
					viewJoin.setPath(String.join(".", Arrays.copyOfRange(propArray, 0, i + 1)));
					viewJoin.setTable(prev.getTable());
					viewJoin.setColumn(prev.getProp(lastRefModule.getProp()).getColumn());
					viewJoin.setRefTable(ref.getTable());
					viewJoin.setRefColumn(ref.getProp(lastRefModule.getRefProp()).getColumn());
					joins.add(viewJoin);
					prev = ref;
					if(i == propArray.length - 1) {
						last = ref.getProp(ref.getPk());
					}
				}
			}
		}
		return last;
	}
	
}
