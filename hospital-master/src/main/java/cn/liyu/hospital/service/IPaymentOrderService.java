package cn.liyu.hospital.service;

import cn.liyu.hospital.dto.PaymentOrderDTO;
import cn.liyu.hospital.entity.PaymentOrder;

import java.util.List;
import java.util.Optional;

/**
 * 支付订单服务接口
 *
 * @author 医秒通
 */
public interface IPaymentOrderService {

    /** 创建支付订单（返回支付订单ID） */
    Long createPaymentOrder(Long accountId, Long appointmentId, Long amount);

    /** 执行支付（分布式锁 + 乐观锁 + 状态机） */
    PaymentOrderDTO pay(Long paymentId, Long accountId);

    /** 查询支付订单 */
    Optional<PaymentOrder> getById(Long paymentId);

    /** 查询支付订单DTO */
    PaymentOrderDTO getPaymentOrderDTO(Long paymentId);

    /** 根据预约编号查询支付订单DTO */
    PaymentOrderDTO getPaymentOrderDTOByAppointment(Long appointmentId);

    /** 分页查询用户支付订单 */
    List<PaymentOrderDTO> listByAccount(Long accountId, Integer status, Integer pageNum, Integer pageSize);

    /** 更新支付状态 */
    boolean updateStatus(Long paymentId, Integer newStatus);

    /** 退款 */
    PaymentOrderDTO refund(Long paymentId, String reason);

    /** 超时关闭过期订单 */
    int closeExpiredOrders();

    /** 判断支付订单是否存在 */
    boolean count(Long paymentId);
}
