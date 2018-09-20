package com.my.dataconvert.server;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.my.dataconvert.connPool.SerConnInstance;
import com.my.dataconvert.constants.Commands;
import com.my.dataconvert.constants.DeviceConstants;
import com.my.dataconvert.constants.TableConstants;
import com.my.dataconvert.manager.JdbcManager;
import com.my.dataconvert.model.DeviceEntity;
import com.my.dataconvert.model.TableEntity;
import com.my.dataconvert.utils.Config;
import com.my.dataconvert.utils.DateUtils;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
/**
 * @author 997620782@qq.com
 */
public class SegementManager {
	private final static Logger log = LoggerFactory.getLogger(SegementManager.class);
	
	//迁移id段的超时时间（单位min）
	public final static int SEGEMENT_TIMEOUT = Config.getInt("segment_timeout");
	//判读id段是否超时，线程的间隔时间（单位second）
	public final static int SEGEMENT_INTERVAL = 20;
	//limit起止 跨度 默认100
	public final static int SEGEMENT_ID = Config.getInt("segment");
	//用于string的synchronized
	private final static Interner<String> pool = Interners.newWeakInterner();
	
	public static String get_segment(String request) {
		String[] bodies = ServerHandler.splitCommand(request, 4);
		if(null == bodies) {
			log.error("请求命令" + Commands.GET_SEGEMENT + "的参数错误！");
			return "请求命令" + Commands.GET_SEGEMENT + "的参数错误！";
		}
		//每次请求id段，都对连接设备的时间进行更新，便于判断是否超时
		DeviceEntity de = DeviceConstants.devices.get(bodies[1] + bodies[2]);
		if(null != de) {
			de.setUpdate_time(DateUtils.currentLong());
		}
		
		String table = bodies[0];
		long min = 0;
		long max = 0;
		synchronized (pool.intern(table)) {
			TableEntity te = TableConstants.tables.get(table);
			if(null != te && te.getCurrent_id() <= te.getMax_id()) {
				min = te.getCurrent_id();
				max = min + SEGEMENT_ID - 1;
				if(max > te.getMax_id()) {
					max = te.getMax_id();
				}
				String sql = "insert into migration_id_segment(tables,min,max,status,create_time) values ('" 
						+ table + "'," + min + "," + max + "," + Commands.STATUS_PREPARE + ",'" + DateUtils.currentDateStr() + "');";
				String update_current_id = "update migration_id_current set current=" + (max + 1) + " where tables='" + table + "'";
				if(JdbcManager.update(SerConnInstance.getInstance(), sql)) {
					if(JdbcManager.update(SerConnInstance.getInstance(), update_current_id)) {
						te.setCurrent_id(max + 1);
					} else {
						//删除上面插入的migration_id_segment
						//这里删除失败了也没事儿， 因为有超时设置在，超时将置为失败
						update_status_db(table, String.valueOf(min), String.valueOf(max), Commands.STATUS_SUCC);
					}
				} else {
					//获取失败
					log.info("失败,获取id段,表名:" + table + ",设备信息:" + bodies[1] + ",线程信息:" + bodies[2]);
					return Commands.return_response(bodies[3], Commands.SEGEMENT_FAIL);
				}
			}
		}
		if(0 != min && 0 != max) {
			log.info("成功,获取id段,表名:" + table + ",limit起止:" + min + "-" + max + ",设备信息:" + bodies[1] + ",线程信息:" + bodies[2]);
			return Commands.return_response(bodies[3], min + "|" + max);
		}
		//current id全部迁移完成，接下来获取迁移失败的
		String sql = "select min,max from migration_id_segment where status=" + Commands.STATUS_FAIL 
				+ " and tables='" + table + "';";
		synchronized (pool.intern(table)) {
			Map<String, String> map = JdbcManager.queryOneMap(SerConnInstance.getInstance(), sql);
			if(null != map && map.size() > 0) {
				String min_id = map.get("min");
				String max_id = map.get("max");
				if(update_status_db(table, min_id, max_id, Commands.STATUS_PREPARE)) {
					min = Long.parseLong(min_id);
					max = Long.parseLong(max_id);
				} else {
					//获取失败
					log.info("失败,获取id段,表名:" + table + ",设备信息:" + bodies[1] + ",线程信息:" + bodies[2]);
					return Commands.return_response(bodies[3], Commands.SEGEMENT_FAIL);
				}
			}
		}
		if(min == 0 && max == 0) {
			//将迁移成功的表 从TableConstants.tables 中删除, 并将current_id 改为 -1
			//false 还存在未成功的id段
			if(!TableConstants.updateTableSucc(table)) {
				log.info("迁移失败的数据");
				return Commands.return_response(bodies[3], "-2|" + SEGEMENT_TIMEOUT * 60 + SEGEMENT_INTERVAL);
			}
			
			log.info("迁移完成，表名:" + table);
			return Commands.return_response(bodies[3], Commands.SEGEMENT_SUCC);
		} else {
			log.info("成功,获取id段,但是数据必须逐条比对，表名:" + table + ",limit起止:" + min + "-" + max + ",设备信息:" + bodies[1] + ",线程信息:" + bodies[2]);
			return Commands.return_response(bodies[3], min + "|" + max + "|fail");
		}
	}
	
	public static String update_status(String request) {
		String[] bodies = ServerHandler.splitCommand(request, 5);
		if(null == bodies) {
			log.error("请求命令" + Commands.UPDATE_STATUS + "的参数错误！");
			return "请求命令" + Commands.UPDATE_STATUS + "的参数错误！";
		}
		String status = bodies[3];
		if(update_status_db(bodies[0], bodies[1], bodies[2], status)) {
			return Commands.return_response(bodies[4], Commands.SUCC);
		}
		
		log.error("更新迁移id段的状态失败，表名:" + bodies[0] + ",min=" + bodies[1] + ",max=" + bodies[2] + ",status=" + status);
		return Commands.return_response(bodies[4], Commands.ERROR);
	}
	
	private static boolean update_status_db(String table, String min, String max, String status) {
		if(Commands.STATUS_SUCC.equals(status)) {
			String sql = "delete from migration_id_segment where tables='"
					+ table + "' and min=" + min + " and max=" + max;
			if(JdbcManager.delete(SerConnInstance.getInstance(), sql)) {
				return true;
			}
		} else if(Commands.STATUS_FAIL.equals(status) || Commands.STATUS_PREPARE.equals(status)) {
			String sql = "update migration_id_segment set status=" + status + " where tables='" 
					+ table + "' and min=" + min + " and max=" + max;
			if(JdbcManager.update(SerConnInstance.getInstance(), sql)) {
				return true;
			}
		}
		return false;
	}
}
