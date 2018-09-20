package com.my.dataconvert.constants;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;

import com.my.dataconvert.connPool.ConnInstance;
import com.my.dataconvert.connPool.SerConnInstance;
import com.my.dataconvert.exception.ConfigException;
import com.my.dataconvert.manager.JdbcManager;
import com.my.dataconvert.model.DeviceEntity;
import com.my.dataconvert.model.TableEntity;
import com.my.dataconvert.utils.Config;
import com.my.dataconvert.utils.DateUtils;

import lombok.extern.slf4j.Slf4j;
/**
 * @author 997620782@qq.com
 */
@Slf4j
public class TableConstants {
	
	public final static Map<String, TableEntity> tables = new ConcurrentHashMap<String, TableEntity>();
	
	public static void init() throws Exception {
		create_db();
		init_config();
		init_db();
	}
	
	/**
	 * 
	 */
	private static void create_db() {
		//创建需要用于迁移数据的表
		boolean flag = JdbcManager.update(SerConnInstance.getInstance(), "create table IF NOT EXISTS `migration_id_segment`  (\n" + 
				"  `id` int(11) not null auto_increment comment '主键id',\n" + 
				"  `tables` varchar(32) not null comment '数据库表名',\n" + 
				"  `min` int(11) default 0 comment '开始id',\n" + 
				"  `max` int(11) default 0 comment '结束id',\n" + 
				"  `status` int(11) default 0 comment '状态 0:准备 1:失败 2:成功(删除)',\n" + 
				"  `create_time` datetime default null comment '创建时间',\n" + 
				"  primary key (`id`),\n" + 
				"  UNIQUE KEY `UK_segment` (`tables`,`min`,`max`) \n" + 
				") engine=innodb default charset=utf8 comment='迁移表，存储id段';");
		log.info("表【MIGRATION_ID_SEGMENT】{}",flag?"创建成功":"创建失败");
		flag = JdbcManager.update(SerConnInstance.getInstance(), "create table IF NOT EXISTS `migration_id_current` (\n" + 
				"  `id` int(11) not null auto_increment comment '主键id',\n" + 
				"  `tables` varchar(32) not null comment '数据库表名',\n" + 
				"  `current` int(11) default 0 comment '当前id, id=-1说明迁移完成',\n" + 
				"  `ability` int(11) default 0 comment '能力值',\n" + 
				"  `create_time` datetime default null comment '创建时间',\n" + 
				"  primary key (`id`),\n" + 
				"  UNIQUE KEY `UK_tables` (`tables`) \n" + 
				") engine=innodb default charset=utf8 comment='迁移表，存储当前id';");
		log.info("表【MIGRATION_ID_CURRENT】{}",flag?"创建成功":"创建失败");
	}
	
	public static void init_db() {
		List<Map<String, String>> results = JdbcManager.queryMap(SerConnInstance.getInstance(), "select * from migration_id_current");
		if(null != results && results.size() > 0) {
			for(Map<String, String> map : results) {
				if(null == map) {
					continue;
				}
				String table_from = map.get("tables");
				TableEntity table = tables.get(table_from);
				if(null == table) {
					log.error("数据库记录和配置文件不一致！表名:" + table_from);
					continue;
				}
				long current_id = Long.parseLong(map.get("current"));
				int ability = Integer.parseInt(map.get("ability"));
				//说明 当前表迁移完成
				if(current_id == -1) {
					tables.remove(table_from);
					table = null;
					log.info("数据库表 " + table_from + " 已经迁移完成。");
					continue;
				}
				//异常数据处理
				if(current_id < 1) {
					log.error("数据库记录异常，current_id=" + current_id);
					current_id = table.getMin_id();
					
				}
				if(0 != ability) {
					String id = map.get("id");
					String sql = "update migration_id_current set ability=0 where id=" + id;
					if(JdbcManager.update(SerConnInstance.getInstance(), sql)) {
						log.info("成功，将表" + table_from + " 的能力值修改为0！原先能力值为" + ability);
					} else {
						log.error("失败，将表" + table_from + " 的能力值修改为0！原先能力值为" + ability);
					}
				}
				table.setCurrent_id(current_id);
				table.setAbility(0);
				//标记下，数据库中存在
				table.setMark(1);
				log.info("表" + table_from + " 在数据库中已经存在！");
			}
		}
		//将配置文件中存在，数据库中不存在的数据 存储
		for(TableEntity table : tables.values()) {
			if(table.getMark() == 1) {
				continue;
			}
			String sql = "insert into migration_id_current(tables,current,ability,create_time) "
					+ "values ('" + table.getTable_from() + "'," + table.getCurrent_id() + "," 
					+ table.getAbility() + ",'" + DateUtils.currentDateStr() + "');";
			if(JdbcManager.update(SerConnInstance.getInstance(), sql)) {
				log.info("表 " + table.getTable_from() + " 成功插入！");
			} else {
				log.error("表 " + table.getTable_from() + " 插入失败，请检查！");
			}
		}
	}
	
	public static void init_config() throws Exception {
		String tables_from = Config.getSetting("tables_from");
		String tables_to = Config.getSetting("tables_to");
		
		String[] tables_from_arr = StringUtils.split(tables_from, "|");
		String[] tables_to_arr = StringUtils.split(tables_to, "|");
		if (null == tables_from_arr || null == tables_to_arr || tables_from_arr.length == 0 
				|| tables_from_arr.length != tables_to_arr.length) {
			throw new ConfigException("config tables_from or tables_to error!");
		}
		
		for(int i=0; i<tables_from_arr.length; i++) {
			TableEntity table = new TableEntity(tables_from_arr[i], tables_to_arr[i]);
			try {
				table.setColumn_from(Config.getSetting(tables_from_arr[i] + "_from"));
				table.setColumn_to(Config.getSetting(tables_to_arr[i] + "_to"));
				//设置最大值
				table.setMin_id(Long.parseLong(Config.getSetting(tables_from_arr[i]+"_index")));
				table.setAbility(0);
				table.setCurrent_id(table.getMin_id());
				table.setTable_from_pageSize(Config.getSetting(tables_from_arr[i]+"_pageSize"));
				table.setTable_from_where(Config.getSetting(tables_from_arr[i]+"_from_where"));
				Map<String, String> map = JdbcManager.queryOneMap(ConnInstance.getFromInstance(), "select count(1) maxnum from "+tables_from_arr[i]);
				if(map == null || map.size() == 0) {
					log.info("表 {} 无数据，不需要迁移" ,tables_from_arr[i]);
					table.setCurrent_id(-1);
				}else {
					table.setMax_id(Long.parseLong(map.get("maxnum")));
				}
				tables.put(tables_from_arr[i], table);
			} catch(Exception e) {
				log.error("数据库表的配置文件错误！", e);
				throw e;
			}
		}
	}
	
	public static String nextTable() {
		//获取下一个迁移表之前，先判断能力值是否超时
		timeout();
		
		Collection<TableEntity> ts = tables.values();
		//所有表都迁移完成
		if(null == ts || ts.size() == 0) {
			return Commands.TABLE_TABLE;
		}
		TableEntity[] tes = ts.toArray(new TableEntity[ts.size()]);
		Arrays.sort(tes);
		return tes[0].getTable_from();
	}
	
	public static void timeout() {
		Collection<DeviceEntity> des = DeviceConstants.devices.values();
		if(null == des || des.size() == 0) {
			return;
		}
		
		for(DeviceEntity device : des) {
			if(DateUtils.isTimeout(device.getUpdate_time(), DeviceConstants.DEVICE_TIMEOUT)) {
				if(reduceAbility(device)) {
					device = null;
				}
			}
		}
	}
	
	public static boolean reduceAbility(DeviceEntity device) {
		if(null == device) {
			return false;
		}
		
		TableEntity table = tables.get(device.getTables());
		//表 迁移完成
		if(null == table) {
			return false;
		}
		
		int ability = table.getAbility() - device.getAbility();
		if(ability < 0) {
			log.error("能力值管理出现错误！");
			ability = 0;
		}
		//将修改后的能力值 更新到数据库
		String sql = "update migration_id_current set ability=" + ability + " where tables='" + table.getTable_from() + "';";
		if(JdbcManager.update(SerConnInstance.getInstance(), sql)) {
			log.info("成功，将表" + table.getTable_from() + " 的能力值修改为" + ability + ", 原先能力值为" + table.getAbility());
			table.setAbility(ability);
			
			DeviceConstants.devices.remove(device.getKey());
			log.info("超时，成功移除设备。" + device.toString());
			return true;
		} else {
			log.error("失败，将表" + table.getTable_from() + " 的能力值修改为" + ability + ", 原先能力值为" + table.getAbility());
		}
		return false;
	}
	
	public static boolean updateTableSucc(String table) {
		//先判断是否所有id段 都迁移完成
		String select_sql = "select id from migration_id_segment where tables='" + table + "'";
		if(JdbcManager.exist(SerConnInstance.getInstance(), select_sql)) {
			return false;
		}
		
		TableEntity te = tables.remove(table);
		//便于回收
		if(null != te) {
			te = null;
		}
		
		//将current 改为 -1
		String sql = "update migration_id_current set current=-1 where tables='" + table + "'";
		//此处更新 失败也没事儿，不影响程序的正确性
		if(JdbcManager.update(SerConnInstance.getInstance(), sql)) {
			return true;
		}
		return false;
	}
	
	public static boolean addAbility(String table, int ability) {
		TableEntity te = tables.get(table);
		if(null == te) {
			return false;
		}
		String sql = "update migration_id_current set ability=ability+" + ability + " where tables='" + table + "'";
		if(JdbcManager.update(SerConnInstance.getInstance(), sql)) {
			te.setAbility(te.getAbility() + ability);
			log.info("成功，将表" + table + " 的能力值修改为" + ability);
		} else {
			log.error("失败，将表" + table + " 的能力值修改为" + ability);
		}
		return false;
	}
}
