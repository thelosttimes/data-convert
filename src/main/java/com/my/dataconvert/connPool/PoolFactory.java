package com.my.dataconvert.connPool;

import java.sql.Connection;
/**
 * @author 997620782@qq.com
 */
public interface PoolFactory {

	public Connection getConnection() throws Exception;
	
	public void releaseConnection(Connection conn) throws Exception;
}
