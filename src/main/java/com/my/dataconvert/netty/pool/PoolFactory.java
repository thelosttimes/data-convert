package com.my.dataconvert.netty.pool;

import com.my.dataconvert.netty.Client;
/**
 * @author 997620782@qq.com
 */
public interface PoolFactory {

	public Client getConnection() throws Exception;

	public void releaseConnection(Client conn) throws Exception;
}
