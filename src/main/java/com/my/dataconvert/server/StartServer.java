package com.my.dataconvert.server;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.my.dataconvert.constants.TableConstants;
import com.my.dataconvert.netty.NettyServer;
import com.my.dataconvert.utils.Config;
import com.my.dataconvert.utils.NamedThreadFactory;

import lombok.extern.slf4j.Slf4j;
/**
 * @author 997620782@qq.com
 */
@Slf4j
@Component
@Order(1)
public class StartServer implements ApplicationRunner{
	
	public  void main1(String[] args) throws Exception {
		log.info("\r\n\t      Migration-tool instance start, port:" + Config.getInt("port") + " [version 1.0-SNAPSHOT] \r\n\t\t\t Copyright (C) 2018 wuhongsheng@fenqigo.com");
		//insert db
		TableConstants.init();
		
		log.info("初始化数据库完毕");
		//定时判断id段是否超时
		ScheduledExecutorService scheduleExec = Executors.newScheduledThreadPool(1);
		scheduleExec.scheduleAtFixedRate(new SegementTimeoutTask(), 0, SegementManager.SEGEMENT_INTERVAL, TimeUnit.SECONDS);
		
		final NettyServer nettyServer = new NettyServer();
		
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					nettyServer.stop();
					log.info("shutdown server");
				} catch (Exception e) {
					log.error("nettyServer stop error!", e);
				}
			}
		}));
		
		ThreadFactory tf = new NamedThreadFactory("BUSINESSTHREAEFACTORY");
		ExecutorService threadPool = new ThreadPoolExecutor(20, 100, 30, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), tf);
		nettyServer.start(Config.getInt("port"), threadPool, Config.getLong("timeout"));
		log.info("服务端启动完毕。。。。。。");
	}

	/**
	 * @param args
	 * @throws Exception
	 * @see org.springframework.boot.ApplicationRunner#run(org.springframework.boot.ApplicationArguments)
	 */
	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("服务端开始启动。。。。。。");
		main1(null);
	}

	
	
}
