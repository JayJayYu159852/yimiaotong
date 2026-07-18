package cn.liyu.hospital.component;

import cn.liyu.hospital.service.IPaymentOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 支付超时订单关闭定时任务
 * <p>
 * 每分钟扫描 payment_order 表，将 status=UNPAID 且 expire_time < NOW() 的订单标记为 EXPIRED。
 * <p>
 * 等同于微信支付的「订单过期自动关闭」机制——用户未在规定时间内完成支付，订单自动失效。
 *
 * @author 医秒通
 */
@Component
public class PaymentTimeoutTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentTimeoutTask.class);

    @Resource
    private IPaymentOrderService paymentOrderService;

    /**
     * 每分钟执行一次：关闭超时未支付的订单
     * <p>
     * Cron 表达式：秒 分 时 日 月 周
     * "0 {@literal *}{@literal /}1 * * * ?" = 每分钟第0秒执行
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void closeExpiredOrders() {
        try {
            int count = paymentOrderService.closeExpiredOrders();
            if (count > 0) {
                LOGGER.info("[定时任务] 已关闭 {} 笔超时支付订单", count);
            }
        } catch (Exception e) {
            LOGGER.error("[定时任务] 关闭超时支付订单异常", e);
        }
    }
}
