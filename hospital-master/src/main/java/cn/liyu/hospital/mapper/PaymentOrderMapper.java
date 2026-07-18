package cn.liyu.hospital.mapper;

import cn.liyu.hospital.entity.PaymentOrder;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 支付订单 Mapper
 * @author 医秒通
 */
public interface PaymentOrderMapper {
    int deleteByPrimaryKey(Long id);

    int insert(PaymentOrder record);

    int insertSelective(PaymentOrder record);

    PaymentOrder selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(PaymentOrder record);

    int updateByPrimaryKey(PaymentOrder record);

    /**
     * 根据支付单号查询
     */
    PaymentOrder selectByPaymentNo(@Param("paymentNo") String paymentNo);

    /**
     * 根据账号查询支付订单列表
     */
    List<PaymentOrder> selectByAccountId(@Param("accountId") Long accountId,
                                          @Param("status") Integer status);

    /**
     * 查询过期的待支付订单
     */
    List<PaymentOrder> selectExpiredOrders(@Param("status") Integer status,
                                            @Param("now") java.util.Date now);

    /**
     * 根据预约编号查询支付订单
     */
    PaymentOrder selectByAppointmentId(@Param("appointmentId") Long appointmentId);
}
