package cn.liyu.hospital.service;

import cn.liyu.hospital.entity.PaymentFlow;

import java.util.List;

/**
 * 支付流水服务接口
 *
 * @author 医秒通
 */
public interface IPaymentFlowService {

    /** 记录支付流水 */
    void recordFlow(Long paymentId, String paymentNo, Long accountId,
                    Long amount, Long balanceBefore, Long balanceAfter,
                    Integer flowType, String remark);

    /** 分页查询流水 */
    List<PaymentFlow> listByAccount(Long accountId, Integer pageNum, Integer pageSize);
}
