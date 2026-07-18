package cn.liyu.hospital.component;

import cn.hutool.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket服务：预约成功/取消预约实时推送管理端
 *
 * @author 医秒通
 */
@Component
@ServerEndpoint("/ws/{sid}")
public class WebSocketServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketServer.class);

    /**
     * 存放会话对象（ConcurrentHashMap 保证并发安全）
     */
    private static final Map<String, Session> SESSION_MAP = new ConcurrentHashMap<>();

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        LOGGER.info("客户端建立连接: sid={}", sid);
        SESSION_MAP.put(sid, session);
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {
        LOGGER.info("收到客户端消息: sid={}, message={}", sid, message);
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose(@PathParam("sid") String sid) {
        LOGGER.info("连接断开: sid={}", sid);
        SESSION_MAP.remove(sid);
    }

    /**
     * 群发消息（向所有在线客户端推送）
     *
     * @param message 消息内容（JSON字符串）
     */
    public void sendToAllClient(String message) {
        Collection<Session> sessions = SESSION_MAP.values();
        for (Session session : sessions) {
            try {
                // 服务器向客户端发送消息
                session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                LOGGER.error("WebSocket消息推送失败: message={}", message, e);
            }
        }
    }

    /**
     * 推送预约提醒消息给管理端（对标苍穹外卖来单/催单提醒）
     *
     * @param type          消息类型：1 预约成功提醒，2 取消预约提醒
     * @param appointmentId 预约编号
     * @param content       消息内容
     */
    public void sendAppointmentMessage(Integer type, Long appointmentId, String content) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("type", type);
            map.put("appointmentId", appointmentId);
            map.put("content", content);
            sendToAllClient(JSONUtil.toJsonStr(map));
        } catch (Exception e) {
            // WebSocket 推送失败不影响主流程
            LOGGER.error("WebSocket推送失败: appointmentId={}, type={}", appointmentId, type, e);
        }
    }

}
