package org.needleframe.core.repository;

import java.io.Serializable;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.h2.util.StringUtils;
import org.needleframe.core.jdbc.DeleteBuilder;
import org.needleframe.core.jdbc.InsertBuilder;
import org.needleframe.core.jdbc.QueryBuilder;
import org.needleframe.core.jdbc.QueryBuilder.SqlClause;
import org.needleframe.core.jdbc.QueryFilter;
import org.needleframe.core.jdbc.QueryProp;
import org.needleframe.core.jdbc.QuerySort;
import org.needleframe.core.jdbc.UpdateBuilder;
import org.needleframe.core.model.Module;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class DataRepository {
	
	@Autowired	
	private EntityManager em;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	public <T> T getEntity(Class<T> clazz, Serializable id) {
		return em.getReference(clazz, id);
	}
	
	public Map<String,Object> getEntity(Module module, List<QueryProp> queryProps, List<QueryFilter> queryFilters) {
		QueryBuilder queryBuilder = new QueryBuilder(module, queryProps, queryFilters, "", Arrays.asList());
		SqlClause<Object> sqlClause = queryBuilder.build();
		List<Object> parameters = sqlClause.getParameters();
		Query query = em.createNativeQuery(sqlClause.getSql());
		for(int i = 0; i < parameters.size(); i++) {
			query.setParameter(i + 1, parameters.get(i));
		}
		Object data = (Object) query.getSingleResult();
		return toMap(sqlClause.getColumnProps(), data);
	}
	
	public Page<Map<String,Object>> findPage(Module module, List<QueryProp> queryProps, 
			List<QueryFilter> queryFilters, List<QuerySort> sortList, String boolFilter, Pageable pageable) {
		QueryBuilder queryBuilder = new QueryBuilder(module, queryProps, queryFilters, boolFilter, sortList);
		SqlClause<Object> sqlClause = queryBuilder.build();
		List<Object> parameters = sqlClause.getParameters();
		Query query = em.createNativeQuery(sqlClause.getSql());
		Query countQuery = em.createNativeQuery(sqlClause.getCountSql());
		for(int i = 0; i < parameters.size(); i++) {
			query.setParameter(i + 1, parameters.get(i));
			countQuery.setParameter(i + 1, parameters.get(i));
		}
		List<?> results = query
				.setFirstResult(new Long(pageable.getOffset()).intValue())
				.setMaxResults(pageable.getPageSize())
				.getResultList();
		BigInteger total = (BigInteger) countQuery.getSingleResult();
		List<Map<String,Object>> content = new ArrayList<Map<String,Object>>();
		for(int i = 0; i < results.size(); i++) {
			Object data = (Object) results.get(i);
			Map<String,Object> dataMap = toMap(sqlClause.getColumnProps(), data);
			content.add(dataMap);
		}
		return new PageImpl<Map<String,Object>>(content, pageable, total.longValue());
	}
	
	public List<Map<String,Object>> findList(Module module, List<QueryProp> queryProps, 
			List<QueryFilter> queryFilters, List<QuerySort> sortList, String boolFilter, Pageable pageable) {
		QueryBuilder queryBuilder = new QueryBuilder(module, queryProps, queryFilters, boolFilter, sortList);
		SqlClause<Object> sqlClause = queryBuilder.build();
		List<Object> parameters = sqlClause.getParameters();
		Query query = em.createNativeQuery(sqlClause.getSql());
		for(int i = 0; i < parameters.size(); i++) {
			query.setParameter(i + 1, parameters.get(i));
		}
		List<?> results = query
				.setFirstResult(new Long(pageable.getOffset()).intValue())
				.setMaxResults(pageable.getPageSize())
				.getResultList();
		List<Map<String,Object>> content = new ArrayList<Map<String,Object>>();
		for(int i = 0; i < results.size(); i++) {
			Object data = (Object) results.get(i);
			Map<String,Object> dataMap = toMap(sqlClause.getColumnProps(), data);
			content.add(dataMap);
		}
		return content;
	}
	
	public int save(Module module, List<Map<String,Object>> dataList) {
		InsertBuilder insertBuilder = new InsertBuilder(module);
		SqlClause<List<Object>> sqlClause = insertBuilder.build(dataList);
		
		GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
		List<List<Object>> parameters = sqlClause.getParameters();
		
		int totalCount = 0;
		for(int j = 0; j < parameters.size(); j++) {
			List<Object> data = parameters.get(j);
			int count = jdbcTemplate.update(new PreparedStatementCreator() {
				public PreparedStatement createPreparedStatement(Connection conn) throws SQLException {
					PreparedStatement psst = conn.prepareStatement(sqlClause.getSql(), Statement.RETURN_GENERATED_KEYS);
					for(int i = 0; i < data.size(); i++) {
						psst.setObject(i + 1, data.get(i));
					}
					return psst;
				}
			}, keyHolder);
			dataList.get(j).put(module.getPk(), keyHolder.getKey().longValue());
			totalCount += count;
		}
		return totalCount;
	}
	
	public int update(Module module, List<Map<String,Object>> dataList) {
		UpdateBuilder sqlBuilder = new UpdateBuilder(module);
		SqlClause<List<Object>> sqlClause = sqlBuilder.build(dataList);
		Query query = em.createNativeQuery(sqlClause.getSql());
		List<List<Object>> parameters = sqlClause.getParameters();
		int totalUpdate = 0;
		for(int i = 0; i < parameters.size(); i++) {
			List<Object> record = parameters.get(i);
			for(int j = 0; j < record.size(); j++) {
				query.setParameter(j + 1, record.get(j));
			}
			int count = query.executeUpdate();
			totalUpdate += count;
		}
		return totalUpdate;
	}
	
	public int delete(Module module, Serializable[] ids) {
		DeleteBuilder sqlBuilder = new DeleteBuilder(module);
		SqlClause<Serializable> sqlClause = sqlBuilder.build(ids);
		List<Serializable> parameters = sqlClause.getParameters();
		Query query = em.createNativeQuery(sqlClause.getSql());
		int totalUpdate = 0;
		for(int i = 0; i < parameters.size(); i++) {
			Serializable id = parameters.get(i);
			query.setParameter(1, id);
			int count = query.executeUpdate();
			totalUpdate += count;
		}
		return totalUpdate;
	}
		
	private Map<String,Object> toMap(List<String> columns, Object data) {
		Map<String,Object> dataMap = new LinkedHashMap<String,Object>();
		Object[] dataArray = columns.size() == 1 ? new Object[] {data} : (Object[]) data;
		for(int j = 0; j < columns.size(); j++) {
			String path = columns.get(j);
			dataMap.put(StringUtils.replaceAll(path, "_", "."), dataArray[j]);
		}
		return dataMap;
	}
	
//	private Map<String,Object> toMap(Object data, List<QueryProp> queryProps) {
//		Map<String,Object> dataMap = new LinkedHashMap<String,Object>();
//		Object[] dataArray = queryProps.size() == 1 ? new Object[] {data} : (Object[]) data;
//		for(int j = 0; j < queryProps.size(); j++) {
//			dataMap.put(queryProps.get(j).getPath(), dataArray[j]);
//		}
//		return dataMap;
//	}
	
	/*
	 * private void findMapModelList(String queryString, String countString,
	 * List<Object> parameters, RowMapper<Map<String,Object>> rowMapper, Pageable
	 * pageable) {
	 * 
	 * List<Map<String,Object>> data = jdbcTemplate.query(queryString,
	 * parameters.toArray(), rowMapper); Long count =
	 * jdbcTemplate.queryForObject(countString, Long.class, parameters);
	 * System.out.println(data.size() + ": " + count); }
	 * 
	 * private class PropRowMapper implements RowMapper<Map<String,Object>> {
	 * private List<ViewProp> viewProps = new ArrayList<ViewProp>(); public
	 * PropRowMapper(List<ViewProp> viewProps) { this.viewProps = viewProps; }
	 * 
	 * public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws
	 * SQLException { Map<String,Object> resultMap = new HashMap<String,Object>();
	 * viewProps.forEach(vp -> { try { resultMap.put(vp.getProp(),
	 * rs.getObject(vp.asName())); } catch (SQLException e) { e.printStackTrace(); }
	 * }); return resultMap; } }
	 */
}
