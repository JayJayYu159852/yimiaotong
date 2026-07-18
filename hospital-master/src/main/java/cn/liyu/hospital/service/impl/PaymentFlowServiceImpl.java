package cn.liyu.hospital.service.impl;

import cn.liyu.hospital.entity.PaymentFlow;
import cn.liyu.hospital.mapper.PaymentFlowMapper;
import cn.liyu.hospital.service.IPaymentFlowService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 支付流水服务实现
 *
 * @author 医秒通
 */
@Service
public class PaymentFlowServiceImpl implements IPaymentFlowService {

    @Resource
    private PaymentFlowMapper flowMapper;

    @Override
    public void recordFlow(Long paymentId, String paymentNo, Long accountId,
                           Long amount, Long balanceBefore, Long balanceAfter,
                           Integer flowType, String remark) {
        PaymentFlow flow = new PaymentFlow();
        flow.setPaymentId(paymentId);
        flow.setPaymentNo(paymentNo);
        flow.setAccountId(accountId);
        flow.setAmount(amount);
        flow.setBalanceBefore(balanceBefore);
        flow.setBalanceAfter(balanceAfter);
        flow.setFlowType(flowType);
        flow.setFlowStatus(1);  // 成功
        flow.setRemark(remark);
        flow.setGmtCreate(new Date());

        flowMapper.insertSelective(flow);
    }

    @Override
    public List<PaymentFlow> listByAccount(Long accountId, Integer pageNum, Integer pageSize) {
        // 使用内存分页（简化实现）
        List<PaymentFlow> all = flowMapper.selectByAccountId(accountId);
        if (all == null || all.isEmpty()) {
            return all;
        }
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, all.size());
        if (start >= all.size()) {
            return List.of();
        }
        return all.subList(start, end);
    }
}
