package com.weishao.migration.tool.server.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "数据库连接与表", description = "数据库连接与表参数")
public class QueryTableParams extends ConnectionParams {

	@ApiModelProperty(value = "源表名", required = true, example = "test")
	private String tableNameSrc;
	
	@ApiModelProperty(value = "新表名", required = true, example = "new_test")
	private String tableNameDest;

	public String getTableNameSrc() {
		return tableNameSrc;
	}

	public void setTableNameSrc(String tableNameSrc) {
		this.tableNameSrc = tableNameSrc;
	}

	public String getTableNameDest() {
		return tableNameDest;
	}

	public void setTableNameDest(String tableNameDest) {
		this.tableNameDest = tableNameDest;
	}

	@Override
	public String toString() {
		return "QueryTableParams [tableNameSrc=" + tableNameSrc + ", tableNameDest=" + tableNameDest + "] "+ super.toString();
	}
	
}
