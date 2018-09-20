package com.my.dataconvert.netty.pool;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import com.my.dataconvert.netty.Client;
/**
 * @author 997620782@qq.com
 */
public class NettyPoolFactory implements PoolFactory {

	private GenericObjectPool<Client> pool;

	public NettyPoolFactory(GenericObjectPoolConfig config, String targetIP, int targetPort, int connectTimeout) {
		
		NettyConnFactory factory = new NettyConnFactory(targetIP, targetPort, connectTimeout);
		pool = new GenericObjectPool<Client>(factory, config);
	}
	@Override
	public Client getConnection() throws Exception {
		return pool.borrowObject();
	}

	@Override
	public void releaseConnection(Client conn) throws Exception {
		pool.returnObject(conn);
	}

}
