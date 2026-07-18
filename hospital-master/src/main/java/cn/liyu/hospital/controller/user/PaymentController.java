package cn.liyu.hospital.controller.user;

import cn.liyu.hospital.common.api.CommonPage;
import cn.liyu.hospital.common.api.CommonResult;
import cn.liyu.hospital.common.security.UserHolder;
import cn.liyu.hospital.dto.PaymentFlowDTO;
import cn.liyu.hospital.dto.PaymentOrderDTO;
import cn.liyu.hospital.dto.WalletDTO;
import cn.liyu.hospital.dto.param.PaymentParam;
import cn.liyu.hospital.entity.PaymentFlow;
import cn.liyu.hospital.entity.PaymentWallet;
import cn.liyu.hospital.service.IPaymentFlowService;
import cn.liyu.hospital.service.IPaymentOrderService;
import cn.liyu.hospital.service.IPaymentWalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户端 · 支付控制器
 * <p>
 * 模拟微信支付 / 支付宝支付的用户交互接口：
 * <ul>
 *   <li>查询钱包余额（等同于微信钱包余额查询）</li>
 *   <li>确认支付（等同于唤起微信支付收银台）</li>
 *   <li>查询支付订单（等同于支付记录）</li>
 *   <li>申请退款（等同于微信支付退款申请）</li>
 *   <li>交易流水查询（等同于微信支付账单）</li>
 * </ul>
 *
 * @author 医秒通
 */
@Tag(name = "用户端 · 支付中心", description = "钱包余额 / 支付 / 退款 / 交易流水")
@RestController
@RequestMapping("/payment")
public class PaymentController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentController.class);

    @Resource
    private IPaymentWalletService walletService;

    @Resource
    private IPaymentOrderService paymentOrderService;

    @Resource
    private IPaymentFlowService flowService;

    // ==================== 钱包 ====================

    @Operation(summary = "查询我的钱包余额", description = "返回当前登录用户的虚拟钱包余额，钱包不存在则自动初始化")
    @GetMapping("/wallet/balance")
    public CommonResult<WalletDTO> getBalance() {
        Long accountId = UserHolder.getUser().getId();

        Optional<PaymentWallet> walletOpt = walletService.getByAccountId(accountId);
        if (!walletOpt.isPresent()) {
            // 懒初始化：没有钱包则自动创建（兼容未执行迁移脚本的已有账号）
            walletService.initWallet(accountId);
            walletOpt = walletService.getByAccountId(accountId);
        }
        if (!walletOpt.isPresent()) {
            return CommonResult.failed("钱包初始化失败，请联系管理员");
        }

        PaymentWallet wallet = walletOpt.get();
        WalletDTO dto = new WalletDTO();
        dto.setWalletId(wallet.getId());
        dto.setAccountId(wallet.getAccountId());
        dto.setBalance(wallet.getBalance());
        dto.setBalanceYuan(String.format("%.2f", wallet.getBalance() / 100.0));
        return CommonResult.success(dto);
    }

    // ==================== 支付 ====================

    @Operation(summary = "确认支付", description = "模拟微信支付收银台确认支付，传入 paymentId 和 accountId")
    @PostMapping("/pay")
    public CommonResult<PaymentOrderDTO> pay(@RequestBody PaymentParam param) {
        if (param.getPaymentId() == null || param.getAccountId() == null) {
            return CommonResult.validateFailed("参数不完整");
        }

        try {
            PaymentOrderDTO result = paymentOrderService.pay(param.getPaymentId(), param.getAccountId());
            return CommonResult.success(result);
        } catch (RuntimeException e) {
            LOGGER.error("支付失败: paymentId={}, error={}", param.getPaymentId(), e.getMessage());
            return CommonResult.failed(e.getMessage());
        }
    }

    // ==================== 支付订单查询 ====================

    @Operation(summary = "查询支付订单详情", description = "传入支付订单编号")
    @GetMapping("/order/{id}")
    public CommonResult<PaymentOrderDTO> getOrder(@PathVariable Long id) {
        PaymentOrderDTO dto = paymentOrderService.getPaymentOrderDTO(id);
        if (dto == null) {
            return CommonResult.failed("支付订单不存在");
        }
        return CommonResult.success(dto);
    }

    @Operation(summary = "根据预约编号查询支付订单", description = "用于秒杀成功后前端获取paymentId")
    @GetMapping("/order/findByAppointment")
    public CommonResult<PaymentOrderDTO> findByAppointment(@RequestParam Long appointmentId) {
        PaymentOrderDTO dto = paymentOrderService.getPaymentOrderDTOByAppointment(appointmentId);
        if (dto == null) {
            return CommonResult.failed("支付订单未找到，请稍后再试");
        }
        return CommonResult.success(dto);
    }

    @Operation(summary = "查询我的支付订单列表", description = "支持按状态筛选")
    @GetMapping("/order/list")
    public CommonResult<CommonPage<PaymentOrderDTO>> listOrders(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long accountId = UserHolder.getUser().getId();

        List<PaymentOrderDTO> list = paymentOrderService.listByAccount(accountId, status, pageNum, pageSize);
        return CommonResult.success(CommonPage.restPage(list));
    }

    // ==================== 退款 ====================

    @Operation(summary = "申请退款", description = "传入支付订单编号和退款原因")
    @PostMapping("/refund/{paymentId}")
    public CommonResult<PaymentOrderDTO> refund(@PathVariable Long paymentId,
                                                 @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "用户申请退款") : "用户申请退款";
        try {
            PaymentOrderDTO result = paymentOrderService.refund(paymentId, reason);
            return CommonResult.success(result);
        } catch (RuntimeException e) {
            LOGGER.error("退款失败: paymentId={}, error={}", paymentId, e.getMessage());
            return CommonResult.failed(e.getMessage());
        }
    }

    // ==================== 交易流水 ====================

    @Operation(summary = "查询我的交易流水", description = "支付扣款 / 退款入账记录")
    @GetMapping("/flow/list")
    public CommonResult<CommonPage<PaymentFlowDTO>> listFlows(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long accountId = UserHolder.getUser().getId();

        List<PaymentFlow> flows = flowService.listByAccount(accountId, pageNum, pageSize);
        if (flows == null || flows.isEmpty()) {
            return CommonResult.success(CommonPage.restPage(Collections.emptyList()));
        }

        List<PaymentFlowDTO> dtos = flows.stream().map(f -> {
            PaymentFlowDTO dto = new PaymentFlowDTO();
            dto.setId(f.getId());
            dto.setPaymentId(f.getPaymentId());
            dto.setPaymentNo(f.getPaymentNo());
            dto.setAmount(f.getAmount());
            dto.setBalanceBefore(f.getBalanceBefore());
            dto.setBalanceAfter(f.getBalanceAfter());
            dto.setFlowType(f.getFlowType());
            dto.setFlowTypeDesc(getFlowTypeDesc(f.getFlowType()));
            dto.setRemark(f.getRemark());
            dto.setGmtCreate(f.getGmtCreate());
            return dto;
        }).collect(Collectors.toList());

        return CommonResult.success(CommonPage.restPage(dtos));
    }

    private String getFlowTypeDesc(Integer flowType) {
        if (flowType == null) return "未知";
        switch (flowType) {
            case 1: return "支付扣款";
            case 2: return "退款入账";
            case 3: return "过期退回";
            default: return "未知";
        }
    }
}
