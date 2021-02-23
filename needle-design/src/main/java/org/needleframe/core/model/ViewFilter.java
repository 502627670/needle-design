package org.needleframe.core.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.needleframe.core.exception.ServiceException;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class ViewFilter {
	
	/**
	 * 支持普通属性x，或者级联属性y.b.c
	 */
	private String prop;
	
	private String op = "=";
	
	public ViewFilter() {}
	
	public abstract Object getValue();
	
	public void setOp(Op op) {
		this.op = op.getViewOperator();
	}
	
	public void setOp(String op) {
		this.op = op;
	}
	
	public ViewFilter(String prop, Op op) {
		this.prop = prop;
		this.setOp(op);
	}
	
	public static PropFilter eq(String prop, Object value) {
		return new PropFilter(prop, value);
	}
	
	public static PropFilter neq(String prop, Object value) {
		return new PropFilter(prop, Op.NOT_EQUAL, value);
	}
	
	public static PropFilter isNull(String prop) {
		return new PropFilter(prop, Op.IS_NULL, null);
	}
	
	public static PropFilter rlike(String prop, String value) {
		return new PropFilter(prop, Op.RLIKE, value);
	}
	
	public static PropFilter ge(String prop, Object value) {
		return new PropFilter(prop, Op.LARGE_EQUAL, value);
	}
	
	public static PropFilter gt(String prop, Object value) {
		return new PropFilter(prop, Op.LARGE, value);
	}
	
	public static PropFilter le(String prop, Object value) {
		return new PropFilter(prop, Op.LESS_EQUAL, value);
	}
	
	public static PropFilter lt(String prop, Object value) {
		return new PropFilter(prop, Op.LESS, value);
	}
	
	public static SubFilter in(String prop, SubQuery subQuery) {
		return new SubFilter(prop, Op.IN, subQuery);
	}
	
	@Getter
	public static class PropFilter extends ViewFilter {
		private Object value;
		public PropFilter() {}
		public PropFilter(String prop, Object value) {
			this(prop, Op.EQUAL, value);
		}
		public PropFilter(String prop, Op op, Object value) {
			super(prop, op);
			setValue(value);
		}
		public Object getValue() {
			return value;
		}
		public void setOp(String op) {
			super.setOp(op);
		}
		public void setValue(Object value) {
			this.value = value;
			if(this.value != null) {
				if(this.value instanceof String) {
					this.value = this.value.toString().trim();
				}
				else if(Collection.class.isAssignableFrom(value.getClass())) {
					this.setOp(Op.IN);
				}
			}
		}
	}
	
	@Getter
	public static class SubFilter extends ViewFilter {
		private SubQuery subQuery;
		public SubFilter() {}
		public SubFilter(String prop, Op op, SubQuery subQuery) {
			super(prop, op);
			this.subQuery = subQuery;
		}
		
		public void setOp(String op) {
			super.setOp(op);
		}
		
		public SubFilter addFilter(String prop, Object value) {
			subQuery.addFilter(prop, Op.EQUAL, value);
			return this;
		}
		
		public SubFilter addFilter(String prop, Op op, Object value) {
			subQuery.addFilter(prop, op, value);
			return this;
		}
		
		@JsonIgnore
		public SubQuery getValue() {
			return subQuery;
		}
	}
	
	@Getter
	@Setter
	public static class SubQuery {
		private String module;
		private String prop;
		
		@JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
		private ArrayList<PropFilter> filters = new ArrayList<PropFilter>();
		
		public SubQuery() {}
		
		public SubQuery(String module, String prop) {
			this.module = module;
			this.prop = prop;
		}
		
		public SubQuery addFilter(String prop, Object value) {
			return addFilter(prop, Op.EQUAL, value);
		}
		
		public SubQuery addFilter(String prop, Op op, Object value) {
			this.filters.add(new PropFilter(prop, op, value));
			return this;
		}
		
		@JsonIgnore
		public ViewProp getViewProp() {
			return new ViewProp(prop);
		}
	}
	
	public static enum Op {
		EQUAL("="),
		NOT_EQUAL("<>"),
		LARGE(">"),
		LARGE_EQUAL(">="),
		LESS("<"),
		LESS_EQUAL("<="),
		LIKE(" like "),   // %a%
		LLIKE(" like "),  // %a
		RLIKE(" like "),  // a%
		IN(" in "),
		NOT_IN(" not in "),
		IS_NULL(" is null");
		
		private String operator;
		private static Map<String,Op> opMap = new ConcurrentHashMap<String,Op>();
		
		static {
			opMap.put("=", Op.EQUAL);
			opMap.put("<>", Op.NOT_EQUAL);
			opMap.put(">", Op.LARGE);
			opMap.put(">=", Op.LARGE_EQUAL);
			opMap.put("<", Op.LESS);
			opMap.put("<=", Op.LESS_EQUAL);
			opMap.put("like", Op.LIKE);
			opMap.put("llike", Op.LLIKE);
			opMap.put("rlike", Op.RLIKE);
			opMap.put("in", Op.IN);
			opMap.put("not in", Op.NOT_IN);
			opMap.put("is null", Op.IS_NULL);
		}
		
		private Op(String operator) {
			this.operator = operator;
		}
		
		/** SQL查询的操作符 */
		public String getOperator() {
			return operator;
		}
		
		/** 页面视图的操作符 */
		public String getViewOperator() {
			if(Op.LLIKE.equals(this) || Op.RLIKE.equals(this)) {
				return this.getName().toLowerCase();
			}
			else {
				return this.getOperator();
			}
		}
		
		public String getName() {
			return this.name().toLowerCase();
		}

		public void setOperator(String operator) {
			this.operator = operator;
		}
		
		public static Op toOp(String operator) {
			Op op = opMap.get(operator.trim().toLowerCase());
			if(op == null) {
				throw new ServiceException("操作符“" + operator + "”不是合法的ViewFilter操作符（=,<>,<,<=,>,>=,like,llike,rlike,in,is null）");
			}
			return op;
		}
		
		public boolean match(String operator) {
			if(this.operator.trim().equals(operator.trim().toLowerCase())) {
				return true;
			}
			return false;
		}
		
		public Object wrapValue(Object value) {
			if(value != null) {
				if(this.equals(Op.LIKE)) {
					return new StringBuilder().append("%").append(value).append("%").toString();
				}
				else if(this.equals(Op.RLIKE)) {
					return new StringBuilder().append(value).append("%").toString();
				}
				else if(this.equals(Op.LLIKE)) {
					return new StringBuilder().append("%").append(value).toString();
				}
			}
			return value;
		}
	}
}
