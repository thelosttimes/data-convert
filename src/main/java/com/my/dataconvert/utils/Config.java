package com.my.dataconvert.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
/**
 * @author 997620782@qq.com
 */
@Slf4j
@Component
public class Config {
	private static Environment env ;
//	private static Map<String, String> setting = new ConcurrentHashMap<String, String>();

//	static {
//		iniSetting();
//	}

//	public static synchronized void iniSetting() {
//		File file = new File("config.properties");
//		if (!(file.exists()))
//			iniSetting("config/config.properties");
//		else
//			iniSetting("config.properties");
//		
//		File poolFile = new File("config/pool.properties");
//		if(poolFile.exists()) {
//			iniSetting("config/pool.properties");
//		}
//	}
	

//	public static synchronized void iniSetting(String path) {
//		File file = new File(path);
//		FileInputStream in = null;
//		try {
//			in = new FileInputStream(file);
//			Properties p = new Properties();
//			p.load(in);
//
//			Enumeration<?> item = p.propertyNames();
//			while (item.hasMoreElements()) {
//				String key = (String) item.nextElement();
//				setting.put(key, p.getProperty(key).trim());
//			}
//			in.close();
//		} catch (FileNotFoundException e) {
//			log.error("config file not found at" + file.getAbsolutePath());
//			throw new ConfigException("FileNotFoundException", e);
//		} catch (IOException e) {
//			log.error("config file not found at" + file.getAbsolutePath());
//			throw new ConfigException("IOException", e);
//		} catch (Exception e) {
//			throw new ConfigException("Exception", e);
//		}
//	}

	

//	public static void reload() {
//		try {
//			iniSetting();
//		} catch (ConfigException e) {
//			throw new ConfigException(e.getMessage(), e);
//		}
//	}
	
//	public static String getSetting(String key) {
//		return ((String) setting.get(key));
//	}
//	
//	public static void setSetting(String key, String value) {
//		setting.put(key, value);
//	}
//	
//	public static int getInt(String key) {
//		return Integer.parseInt(setting.get(key));
//	}
//	
//	public static long getLong(String key) {
//		return Long.parseLong(setting.get(key));
//	}
//	
//	public static boolean getBoolean(String key) {
//		return Boolean.parseBoolean(setting.get(key));
//	}

	
	
//	private static Config instance;  
//    private Config (){
//        
//    }   
//    public static synchronized Config getInstance(){    //对获取实例的方法进行同步
//      if (instance == null)     
//        instance = new Config(); 
//      return instance;
//    }

	public static String getSetting(String key) {
		return env.getProperty(key);
	}

	public static int getInt(String key) {
		return Integer.parseInt(env.getProperty(key));
	}
	
	public static long getLong(String key) {
		return Long.parseLong(env.getProperty(key));
	}
	
	public static boolean getBoolean(String key) {
		return Boolean.parseBoolean(env.getProperty(key));
	}
	public Environment getEnv() {
		return Config.env;
	}
	@Autowired(required = true)
	public  void setEnv(Environment env) {
		log.info(env+"==========================");
		Config.env = env;
	}
	
	
}
