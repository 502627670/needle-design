package org.needleframe.core.jdbc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.needleframe.core.jdbc.QueryFilter.QuerySubFilter;
import org.needleframe.core.model.Module;
import org.needleframe.core.model.ViewFilter.Op;
import org.needleframe.core.model.ViewJoin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import lombok.Getter;

public class QueryBuilder {
	
	Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private Module module;
	private List<QueryProp> queryProps = new ArrayList<QueryProp>();
	private List<QueryFilter> queryFilters = new ArrayList<QueryFilter>();
	private List<QuerySort> sortList = new ArrayList<QuerySort>();
	private String boolFilter;
	private List<Object> parameters = new ArrayList<Object>();
	
	private SqlFrom root;
	private Map<String,SqlFrom> sqlFromMap = new LinkedHashMap<String,SqlFrom>();
	private List<SqlProp> sqlProps = new ArrayList<SqlProp>();
	private List<SqlFilter> sqlFilters = new ArrayList<SqlFilter>();
	private List<SqlSort> sqlSorts = new ArrayList<SqlSort>();
	
	public QueryBuilder(Module module, List<QueryProp> props, List<QueryFilter> filters, String boolFilter, List<QuerySort> sortList) {
		this.module = module;
		this.queryProps = props;
		this.queryFilters = filters;
		this.boolFilter = boolFilter;
		this.sortList = sortList;
	}
	
	private QueryBuilder(Module module, List<QueryProp> props, List<QueryFilter> filters, List<Object> parameters) {
		this.module = module;
		this.queryProps = props;
		this.queryFilters = filters;
		this.parameters = parameters;
	}
	
	public SqlClause<Object> build() {
		parse();
		
		String selectSql = selectSql();
		String fromSql = fromSql();
		String whereSql = whereSql();
		String orderSql = orderSql();
		String sql = new StringBuilder(selectSql).append(fromSql).append(whereSql).append(orderSql).toString();
		String countSql = new StringBuilder("select count(distinct ")
				.append(this.root.alias).append(".")
				.append(module.getProp(module.getPk()).getColumn()).append(")")
				.append(fromSql).append(whereSql).toString();
		List<String> pathList = this.sqlProps.stream().map(sp -> sp.path).collect(Collectors.toList());
		SqlClause<Object> sqlClause = new SqlClause<Object>(sql, countSql, this.parameters);
		sqlClause.columnProps = pathList;
		return sqlClause;
	}
	
	private String selectSql() {
		StringBuilder queryString = new StringBuilder("select distinct");
		if(sqlProps.isEmpty()) {
			queryString.append(" *");
		}
		else {
			queryString.append(" ")
				.append(sqlProps.stream().map(sqlProp -> sqlProp.toSql()).collect(Collectors.joining(",")));
		}
		return queryString.toString();
	}
		
	private String fromSql() {
		StringBuilder queryString = new StringBuilder(" from ");
		String rootJoinSql = root.toSql();
		queryString.append(root.table).append(" ").append(root.alias);
		if(rootJoinSql.length() > 0) {
			queryString.append(" ").append(rootJoinSql);
		}
		if(!sqlFromMap.isEmpty()) {
			String sql = sqlFromMap.values().stream().map(sqlFrom -> sqlFrom.toSql())
				.collect(Collectors.joining(" "));
			sql = sql.trim();
			queryString.append(sql.length() > 0 ? " " : "").append(sql);
		}
		return queryString.toString();
	}
	
	private String whereSql() {
		StringBuilder queryString = new StringBuilder(" where 1=1");
		if(sqlFilters.size() > 0) {
			queryString.append(" and (");
			if(!StringUtils.hasText(boolFilter)) { 
				String filterSql = sqlFilters.stream().map(sqlFilter -> sqlFilter.toSql()).collect(Collectors.joining(" and "));
				queryString.append(filterSql.trim());
			}
			else {
				boolFilter = boolFilter.trim();
				String[] numbers = boolFilter.replaceAll("(and|or|\\(|\\))", "").split("[ ]+");
				Set<String> numberSet = new HashSet<String>(Arrays.asList(numbers));
				
				String sql = new String(boolFilter);
				List<String> boolFilterSqls = sqlFilters.subList(0, numberSet.size()).stream()
					.map(filter -> filter.toSql()).collect(Collectors.toList());
				for(int i = boolFilterSqls.size() - 1; i >= 0; i--) {
					String regex = "(?<!\\?)" + (i + 1);
					sql = sql.replaceFirst(regex, boolFilterSqls.get(i));
				}
				queryString.append(sql);
				
				if(sqlFilters.size() > numberSet.size()) {
					String tailSql = sqlFilters.subList(numberSet.size(), sqlFilters.size()).stream()
						.map(filter -> filter.toSql())
						.collect(Collectors.joining(" and "));
					queryString.append(" and ").append(tailSql);
				}
			}
			queryString.append(")");
		}
		
		return queryString.toString();
	}
	
	private String orderSql() {
		StringBuilder sortBuilder = new StringBuilder();
		if(!this.sqlSorts.isEmpty()) {
			sortBuilder.append(" order by ");
			String sql = this.sqlSorts.stream().map(ss -> ss.toSql()).collect(Collectors.joining(","));
			sortBuilder.append(sql);
		}
		return sortBuilder.toString();
	}
	
	/**
	 * x：普通属性
	 * y.b.c：标准关联属性，比如userRole属性关联Role对象的name属性，记为userRole.role.name
	 * z.M.d：特殊关联属性，比如saleId属性关联Sale对象的主键id属性，记为saleId.Sale.id
	 */
	private void parse() {
		final String alias = "t";
		this.root = new SqlFrom(alias, this.module.getTable());
		
		this.parseProps();
		
		this.parseFilters();
		
		this.parseSorts();
	}
	
	private void parseProps() {
		List<String> viewProps = new ArrayList<String>();
		queryProps.stream().forEach(qp -> {
			if(!viewProps.contains(qp.getViewProp().getProp())) {
				viewProps.add(qp.getViewProp().getProp());
				
				List<ViewJoin> viewJoins = qp.getViewJoins();
				SqlFrom sqlFrom = root;
				if(viewJoins.isEmpty()) {
					SqlProp sqlProp = new SqlProp(qp.getPath(), sqlFrom.alias, qp.getLastColumn(), qp.getPath(), true);
					sqlProps.add(sqlProp);
				}
				else {
					for(int i = 0; i < viewJoins.size(); i++) {
						ViewJoin viewJoin = viewJoins.get(i);
						String path = viewJoin.getPath();
						String refTable = viewJoin.getRefTable();
						
						SqlFrom sqlTo = sqlFromMap.get(path);
						if(sqlTo == null) {
							sqlTo = new SqlFrom("t" + sqlFromMap.size(), refTable);
							sqlFromMap.put(path, sqlTo);
							
							LeftJoin leftJoin = sqlFrom.leftJoins.get(sqlTo.alias);
							if(leftJoin == null) {
								leftJoin = new LeftJoin(sqlFrom, sqlTo);
								sqlFrom.leftJoins.put(sqlTo.alias, leftJoin);
							}
							leftJoin.addJoinColumn(new JoinColumn(viewJoin.getColumn(), viewJoin.getRefColumn()));
						}
						
//						SqlFrom sqlTo = sqlFromMap.computeIfAbsent(path, v -> new SqlFrom("t" + sqlFromMap.size(), refTable));
//						LeftJoin leftJoin = sqlFrom.leftJoins.get(sqlTo.alias);
//						if(leftJoin == null) {
//							leftJoin = new LeftJoin(sqlFrom, sqlTo);
//							sqlFrom.leftJoins.put(sqlTo.alias, leftJoin);
//						}
//						leftJoin.addJoinColumn(new JoinColumn(viewJoin.getColumn(), viewJoin.getRefColumn()));
						
						sqlFrom = sqlTo;
					}
					
					SqlProp sqlProp = new SqlProp(qp.getPath(), sqlFrom.alias, qp.getLastColumn(), qp.asName(), true);
					sqlProps.add(sqlProp);
					if(!qp.getShowColumn().equals(qp.getLastColumn()) && !(this instanceof SubQueryBuilder)) {
						sqlProps.add(new SqlProp(qp.getShowPath(), sqlFrom.alias, qp.getShowColumn(), qp.asShowName(), true));
					}
				}
			}
		});
	}
	
	private void parseFilters() {
		queryFilters.stream().forEach(qf -> {
			List<ViewJoin> joins = qf.getViewJoins();
			SqlFrom sqlFrom = root;
			for(int i = 0; i < joins.size(); i++) {
				ViewJoin viewJoin = joins.get(i);
				String path = viewJoin.getPath();
				String refTable = viewJoin.getRefTable();
				
				SqlFrom sqlTo = sqlFromMap.computeIfAbsent(path, v -> new SqlFrom("t" + sqlFromMap.size(), refTable));
				LeftJoin leftJoin = sqlFrom.leftJoins.get(sqlTo.alias);
				if(leftJoin == null) {
					leftJoin = new LeftJoin(sqlFrom, sqlTo);
					sqlFrom.leftJoins.put(sqlTo.alias, leftJoin);
				}
				leftJoin.addJoinColumn(new JoinColumn(viewJoin.getColumn(), viewJoin.getRefColumn()));
				
				sqlFrom = sqlTo;
			}
			
			Object value = qf.getValue();
			if(value instanceof QuerySubFilter) {
				SubQueryBuilder subBuilder = new SubQueryBuilder((QuerySubFilter) value, this.parameters);
				String subSql = subBuilder.build().getSql();
				SqlFilter sqlFilter = new SqlSubFilter(sqlFrom.alias, qf.getLastColumn(), qf.getOp(), subSql);
				sqlFilters.add(sqlFilter);
			}
			else {
				Op op = qf.getOp();
				Object sqlValue = op.wrapValue(qf.getValue());
				SqlFilter sqlFilter = new SqlFilter(sqlFrom.alias, qf.getLastColumn(), op, sqlValue);
				
				sqlFilters.add(sqlFilter);
			}
		});
	}
	
	private void parseSorts() {
		sortList.forEach(qs -> {
			List<ViewJoin> joins = qs.getViewJoins();
			SqlFrom sqlFrom = root;
			if(joins.isEmpty()) {
				SqlSort sqlSort = new SqlSort(sqlFrom.alias, qs.getLastColumn(), qs.getDirection());
				sqlSorts.add(sqlSort);
			}
			else {
				for(int i = 0; i < joins.size(); i++) {
					ViewJoin viewJoin = joins.get(i);
					String path = viewJoin.getPath();
					String refTable = viewJoin.getRefTable();
					
					SqlFrom sqlTo = sqlFromMap.computeIfAbsent(path, v -> new SqlFrom("t" + sqlFromMap.size(), refTable));
					LeftJoin leftJoin = sqlFrom.leftJoins.get(sqlTo.alias);
					if(leftJoin == null) {
						leftJoin = new LeftJoin(sqlFrom, sqlTo);
						sqlFrom.leftJoins.put(sqlTo.alias, leftJoin);
					}
					leftJoin.addJoinColumn(new JoinColumn(viewJoin.getColumn(), viewJoin.getRefColumn()));
					
					sqlFrom = sqlTo;
				}
				SqlSort sqlSort = new SqlSort(sqlFrom.alias, qs.getLastColumn(), qs.getDirection());
				sqlSorts.add(sqlSort);
			}
		});
	}
	
	class SubQueryBuilder extends QueryBuilder {
		public SubQueryBuilder(QuerySubFilter querySubFilter, List<Object> parameters) {
			super(querySubFilter.module, Arrays.asList(querySubFilter.prop), querySubFilter.filters, parameters);
		}
	}
	
	public static class SqlProp {
		String alias;     //  
		String prop;      // column
		String asName;    // a_b_c_name
		String path;      // prop path
		boolean show = true;
		
		SqlProp(String path, String alias, String column, String asName, boolean show) {
			this.path = path;
			this.alias = alias;
			this.prop = column;
			this.asName = asName;
			this.show = show;
		}
		
		public String toSql() {
			StringBuilder sqlBuilder = new StringBuilder();
			sqlBuilder.append(alias).append(".").append(prop);
			if(StringUtils.hasText(asName)) {
				sqlBuilder.append(" as ").append(asName.replaceAll("\\.", "_").toLowerCase() + "0").append("");
			}
			return sqlBuilder.toString();
		}
	}
	
	class SqlFrom {
		String alias;
		String table;
		Map<String,LeftJoin> leftJoins = new LinkedHashMap<String,LeftJoin>();
		
		SqlFrom(String alias, String table) {
			this.alias = alias;
			this.table = table;
		}
		
		public String toSql() {
			StringBuilder sqlBuilder = new StringBuilder();
			String joinSql = leftJoins.values().stream()
				.map(lj -> lj.toJoinSql()).collect(Collectors.joining(" "));
			sqlBuilder.append(joinSql);
			return sqlBuilder.toString();
		}
	}
	
	class JoinColumn {
		String from;
		String to;
		public JoinColumn(String from, String to) {
			this.from = from;
			this.to = to;
		}
	}
	
	class LeftJoin {
		SqlFrom from;
		SqlFrom to;
		private List<JoinColumn> joinColumns = new ArrayList<JoinColumn>();
		
		LeftJoin(SqlFrom from, SqlFrom to) {
			this.from = from;
			this.to = to;
		}
		
		public void addJoinColumn(JoinColumn joinColumn) {
			joinColumns.add(joinColumn);
		}
		
		public String toJoinSql() {
			StringBuilder sqlBuilder = new StringBuilder("left join ");
			sqlBuilder.append(to.table).append(" ").append(to.alias).append(" on ");
			String sql = joinColumns.stream().map(jc -> {
				return new StringBuilder(from.alias).append(".").append(jc.from)
					.append("=").append(to.alias).append(".").append(jc.to).toString();
			}).collect(Collectors.joining(" and "));
			sqlBuilder.append(sql);
			return sqlBuilder.toString();
		}
	}
	
	class SqlJoin {
		SqlFrom from;
		SqlFrom to;
		String fromAlias;
		String toAlias;
		String fromProp;
		String toProp;
		
		SqlJoin(SqlFrom from, SqlFrom to, String fromProp, String toProp) {
			this.from = from;
			this.to = to;
			this.fromAlias = from.alias;
			this.toAlias = to.alias;
			this.fromProp = fromProp;
			this.toProp = toProp;
		}
		
		public String toSql() {
			StringBuilder sqlBuilder = new StringBuilder();
			sqlBuilder.append("(").append(fromAlias).append(".").append(fromProp).append("=");
			StringBuilder toBuilder = new StringBuilder(toAlias);
			if(StringUtils.hasText(toProp)) {
				toBuilder.append(".").append(toProp);
			}
			sqlBuilder.append(toBuilder).append(")");
			return sqlBuilder.toString();
		}
	}
	
	@Getter
	class SqlFilter {
		String alias;
		String prop;
		Op op;
		Object value;
		
		public SqlFilter(String alias, String prop, Op op, Object value) {
			this.alias = alias;
			this.prop = prop;
			this.op = op;
			this.value = value;
		}
		
		public String toSql() {
			StringBuilder queryBuilder = new StringBuilder();
			queryBuilder.append(alias).append(".").append(prop).append(op.getOperator());
			if(!Op.IS_NULL.equals(op)) {
				if(value instanceof Collection) {
					Collection<?> valueList = (Collection<?>) value;
					if(valueList.size() > 0) {
						queryBuilder.append("(");
						for(Iterator<?> iterator = valueList.iterator(); iterator.hasNext();) {
							parameters.add(iterator.next());
							queryBuilder.append("?").append(parameters.size()).append(",");
						}
						queryBuilder = queryBuilder.replace(queryBuilder.length() - 1, queryBuilder.length(), ")");
					}
				}
				else {
					parameters.add(value);
					queryBuilder.append("(?").append(parameters.size()).append(")");
				}
			}
			return queryBuilder.toString();
		}
	}
	
	class SqlSubFilter extends SqlFilter {
		private String subSql;
		public SqlSubFilter(String alias, String prop, Op op, String subSql) {
			super(alias, prop, op, subSql);
			this.subSql = subSql;
		}
		
		public String toSql() {
			StringBuilder queryBuilder = new StringBuilder();
			queryBuilder.append(alias).append(".").append(prop).append(op.getOperator());
			queryBuilder.append("(").append(subSql).append(")");
			return queryBuilder.toString();
		}
	}
	
	class SqlSort {
		String alias;
		String prop;
		String direction;
		
		public SqlSort(String alias, String prop, String direction) {
			this.alias = alias;
			this.prop = prop;
			this.direction = direction;
		}
		
		public String toSql() {
			StringBuilder queryBuilder = new StringBuilder();
			queryBuilder.append(this.alias).append(".").append(this.prop);
			queryBuilder.append(" ").append(this.direction);
			return queryBuilder.toString();
		}
	}
	
	@Getter
	public static class SqlClause<T> {
		String sql;
		String countSql;
		List<T> parameters = new ArrayList<T>();
		List<String> columnProps = new ArrayList<String>();
		
		public SqlClause(String sql, String countSql, List<T> parameters) {
			this.sql = sql;
			this.countSql = countSql;
			this.parameters = parameters;
		}
	}
}
