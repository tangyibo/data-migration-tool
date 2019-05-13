package com.weishao.data.extractor.engine;

import java.util.Map;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransHopMeta;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.steps.tableinput.TableInputMeta;
import org.pentaho.di.trans.steps.tableoutput.TableOutputMeta;

import com.weishao.dmtool.DMTool;
import com.weishao.dmtool.core.JdbcConnector;
import com.weishao.dmtool.core.factory.DataSourceFactory;

public class DataMigrationExtractor {
	
	protected static Log logger = LogFactory.getLog(DataMigrationExtractor.class);
	
	private DMTool dmtool;
	
	private JdbcConnector jdbcSourceConnector;
	private JdbcConnector jdbcTargetConnector;

	public DataMigrationExtractor(DMTool dm, JdbcConnector conn) {
		this.dmtool = dm;
		
		this.jdbcSourceConnector=dm.getSourceJdbcConnector();
		this.jdbcTargetConnector = conn;
	}

	public void doMigration(Map<String, String> tableMappers, boolean createIfNotExist, boolean dropTable) {
		
		try {
			BasicDataSource targetDataSouce=DataSourceFactory.getDataSource(this.jdbcTargetConnector);
			
			for (Map.Entry<String, String> entry : tableMappers.entrySet()) {
				String sourceTableName = entry.getKey();
				String targetTableName = entry.getValue();
				if (null == targetTableName || "".equals(targetTableName) || null == sourceTableName
						|| "".equals(sourceTableName)) {
					throw new RuntimeException("Invalid parameters [tableMappers],target table name can not be empty!");
				}

				// 表结构抽取与映射
				String createTableSql = this.dmtool.getMysqlCreateTableSql(sourceTableName, targetTableName,
						createIfNotExist);

				DefaultTransactionDefinition def = new DefaultTransactionDefinition();
				def.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
				def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
				DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(targetDataSouce);
				TransactionStatus status = transactionManager.getTransaction(def);

				JdbcTemplate jdbcTemplate = new JdbcTemplate(targetDataSouce);
				try {
					if (dropTable) {
						jdbcTemplate.execute(String.format("drop table if exists `%s`", targetTableName));
					}

					jdbcTemplate.execute(createTableSql);

				} catch (TransactionException e) {
					transactionManager.rollback(status);
					throw e;
				} catch (DataAccessException e) {
					transactionManager.rollback(status);
					throw e;
				} catch (RuntimeException e) {
					transactionManager.rollback(status);
					throw e;
				} catch (Exception e) {
					transactionManager.rollback(status);
					throw e;
				} finally {

				}

				// 数据抽取与映射
				boolean ret = exctractTableData(sourceTableName, targetTableName, true);
				if (ret) {
					logger.debug("extract table data over for table:" + sourceTableName);
				} else {
					logger.error("extract table data error for table:" + sourceTableName);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	public boolean exctractTableData(String sourceTableName, String targetTableName, boolean truncateTable)
			throws KettleException {
		try {
			KettleEnvironment.init();
			TransMeta transMeta = new TransMeta();
			transMeta.setName("trans_work");

			DatabaseMeta db_src = new DatabaseMeta("sourceDatabase",
					this.jdbcSourceConnector.getDbType().name().toLowerCase(), "Native",
					this.jdbcSourceConnector.getHostAddress(), this.jdbcSourceConnector.getDbName(),
					String.valueOf(this.jdbcSourceConnector.getServerPort()), this.jdbcSourceConnector.getUserName(),
					this.jdbcSourceConnector.getPassword());

			DatabaseMeta db_dst = new DatabaseMeta("targetDatabase",
					this.jdbcTargetConnector.getDbType().name().toLowerCase(), "Native",
					this.jdbcTargetConnector.getHostAddress(), this.jdbcTargetConnector.getDbName(),
					String.valueOf(this.jdbcTargetConnector.getServerPort()), this.jdbcTargetConnector.getUserName(),
					this.jdbcTargetConnector.getPassword());

			db_dst.addExtraOption(db_dst.getPluginId(), "characterEncoding", "utf8");
			db_src.addExtraOption(db_src.getPluginId(), "zeroDateTimeBehavior", "convertToNull");

			transMeta.addDatabase(db_src);
			transMeta.addDatabase(db_dst);

			TableInputMeta t_input = new TableInputMeta();
			t_input.setDatabaseMeta(transMeta.findDatabase("sourceDatabase"));
			t_input.setSQL(String.format("select * from %s", sourceTableName));
			StepMeta input = new StepMeta("source-table-data-inpurt", t_input);
			transMeta.addStep(input);

			TableOutputMeta t_output = new TableOutputMeta();
			t_output.setDatabaseMeta(transMeta.findDatabase("targetDatabase"));
			t_output.setTableName(targetTableName);
			t_output.setCommitSize(10000);
			t_output.setUseBatchUpdate(true);
			t_output.setTruncateTable(truncateTable);
			StepMeta output = new StepMeta("target-table-data-output", t_output);
			transMeta.addStep(output);

			transMeta.addTransHop(new TransHopMeta(input, output));

			Trans trans = new Trans(transMeta);

			trans.execute(null);
			trans.waitUntilFinished();

			if (0 == trans.getErrors()) {
				return true;
			}
		} catch (KettleException e) {
			logger.error("extract table data error:" + e.getMessage());
			throw e;
		}

		return false;
	}
}
