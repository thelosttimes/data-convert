package com.my.dataconvert.client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.my.dataconvert.utils.Config;
import com.my.dataconvert.utils.NamedThreadFactory;

import lombok.extern.slf4j.Slf4j;
/**
 * @author 997620782@qq.com
 */
@Slf4j
@Component
@Order(2)
public class StartMigration  implements ApplicationRunner{
	
	public  void main1(String[] args) throws Exception {
		log.info("\r\n\t      Migration-tool client instance start, [version 1.0-SNAPSHOT] "
				+ "\r\n\t\t host=" + Config.getSetting("host") + " port=" + Config.getSetting("port") 
				+ " ability=" + Config.getSetting("ability") + " thread_num=" + Config.getSetting("thread_num")
				+ "\r\n\t\t\t Copyright (C) 2018 wuhongsheng@fenqigo.com");
		//提前加载驱动
		Class.forName("com.mysql.jdbc.Driver");
		//
		int nThreads = Integer.parseInt(Config.getSetting("thread_num"));
		ExecutorService threadPool = Executors.newFixedThreadPool(nThreads, new NamedThreadFactory("migration"));
		
		for(int i=0; i<nThreads; i++) {
			threadPool.execute(new MigrationTask());
			try {
				//暂停1s是为了 能力值处理
				Thread.sleep(1000);
			} catch (Exception e) {
				log.error("main thread sleep 1000ms error!",e);
			}
		}
		log.info("客户端启动完毕 。。。。。。");
	}

	/**
	 * @param args
	 * @throws Exception
	 * @see org.springframework.boot.ApplicationRunner#run(org.springframework.boot.ApplicationArguments)
	 */
	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("客户端启动开始 。。。。。。");
		main1(null);
	}
}
