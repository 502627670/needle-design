package org.needleframe.core.jdbc;

import static java.util.Arrays.asList;

import java.io.Serializable;
import java.util.List;

import org.needleframe.core.jdbc.QueryBuilder.SqlClause;
import org.needleframe.core.model.Module;

public class DeleteBuilder {

	private Module module;
	
	public DeleteBuilder(Module module) {
		this.module = module;
	}
	
	public SqlClause<Serializable> build(Serializable[] ids) {
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("delete from ").append(module.getTable());
		sqlBuilder.append(" where ")
			.append(module.getProp(module.getPk()).getColumn())
			.append("=?");
		
		List<Serializable> parameters = asList(ids);
		return new SqlClause<Serializable>(sqlBuilder.toString(), "", parameters);
	}
	
}
