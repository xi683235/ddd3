package com.easylinker.proxy.server.app.config.mqttconfig.handler;

import com.alibaba.fastjson.JSONObject;
import com.easylinker.proxy.server.app.bean.RealTimeMessage;
import com.easylinker.proxy.server.app.config.mqttconfig.MqttMessageSender;
import com.easylinker.proxy.server.app.constants.DailyLogType;
import com.easylinker.proxy.server.app.constants.mqtt.RealTimeType;
import com.easylinker.proxy.server.app.model.daily.DailyLog;
import com.easylinker.proxy.server.app.model.daily.DeviceOnAndOffLineLog;
import com.easylinker.proxy.server.app.model.device.Device;
import com.easylinker.proxy.server.app.service.DailyLogService;
import com.easylinker.proxy.server.app.service.DeviceOnAndOffLineLogService;
import com.easylinker.proxy.server.app.service.DeviceService;
import com.easylinker.proxy.server.app.utils.HttpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 设备上下线控制
 */
@Component
public class ClientOnAndOfflineWillMessageHandler implements MessageHandler {
    Logger logger = LoggerFactory.getLogger(ClientOnAndOfflineWillMessageHandler.class);
    @Autowired
    DeviceService deviceService;
    @Value("${emq.username}")
    private String EMQ_USERNAME;

    @Autowired
    MqttMessageSender mqttMessageSender;
    @Autowired
    DailyLogService dailyLogService;
    @Autowired
    HttpTool httpTool;
    @Value("${emq.api.host}")
    String apiHost;
    //emq.websocket.username
    @Value("${emq.websocket.username}")
    String WEBSOCKET_USERNAME;
    @Autowired
    DeviceOnAndOffLineLogService deviceOnAndOffLineLogService;

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        JSONObject mqttMessage;
        MessageHeaders header = message.getHeaders();
        String state = header.get("mqtt_topic").toString().
                substring(header.get("mqtt_topic").toString().lastIndexOf("/") + 1);
        if (state.equals("connected")) {
            try {
                mqttMessage = (JSONObject) JSONObject.parse(message.getPayload().toString());
                String username = mqttMessage.getString("username");
                if (username.equals(EMQ_USERNAME) || username.equals(WEBSOCKET_USERNAME)) {
                    logger.info("内部组件[" + username + "]连接成功!");
                } else {
                    try {
                        Device device = deviceService.findADevice(Long.parseLong(username));
                        if (device != null) {
                            device.setOnline(true);
                            deviceService.save(device);
                            logger.info("设备:[" + device.getDeviceName() + "]上线");
                            JSONObject realTimeJson = new JSONObject();
                            realTimeJson.put("type", RealTimeType.ONLINE);
                            realTimeJson.put("device", device.getId());
                            mqttMessageSender.sendRealTimePureMessage(realTimeJson);
                            //生成日志
                            DeviceOnAndOffLineLog deviceOnAndOffLineLog = new DeviceOnAndOffLineLog();
                            deviceOnAndOffLineLog.setDate(new Date());
                            deviceOnAndOffLineLog.setDevice(device);
                            deviceOnAndOffLineLog.setEvent("CONNECT");
                            deviceOnAndOffLineLogService.save(deviceOnAndOffLineLog);

                        }
                    } catch (Exception e) {
                        //todo
                        logger.error(e.getMessage());
                    }


                }
            } catch (Exception e) {
                logger.error("解析消息时出现了格式错误!");
            }


        } else if (state.equals("disconnected")) {
            try {
                mqttMessage = (JSONObject) JSONObject.parse(message.getPayload().toString());
                String username = mqttMessage.getString("username");
                if (username.equals(EMQ_USERNAME) || username.equals(WEBSOCKET_USERNAME)) {
                    logger.info("内部组件[" + username + "]断开连接!");
                } else {
                    Device device = deviceService.findADevice(Long.parseLong(username));
                    if (device != null) {
                        device.setOnline(false);
                        device.setLastActiveDate(new Date());
                        deviceService.save(device);
                        logger.info("设备:[" + device.getDeviceName() + "]下线");
                        JSONObject realTimeJson = new JSONObject();
                        realTimeJson.put("type", RealTimeType.OFFLINE);
                        realTimeJson.put("device", device.getId());
                        mqttMessageSender.sendRealTimePureMessage(realTimeJson);
                        //生成日志
                        DeviceOnAndOffLineLog deviceOnAndOffLineLog = new DeviceOnAndOffLineLog();
                        deviceOnAndOffLineLog.setDate(new Date());
                        deviceOnAndOffLineLog.setDevice(device);
                        deviceOnAndOffLineLog.setEvent("DISCONNECT");
                        deviceOnAndOffLineLogService.save(deviceOnAndOffLineLog);

                    }
                }
            } catch (Exception e) {
                logger.error("解析消息时出现了格式错误!");
            }

        }
    }
}
