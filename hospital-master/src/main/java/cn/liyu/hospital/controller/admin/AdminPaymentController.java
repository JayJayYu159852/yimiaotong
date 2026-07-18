package cn.liyu.hospital.controller.admin;

import cn.liyu.hospital.common.api.CommonPage;
import cn.liyu.hospital.common.api.CommonResult;
import cn.liyu.hospital.dto.PaymentOrderDTO;
import cn.liyu.hospital.dto.WalletDTO;
import cn.liyu.hospital.service.IPaymentOrderService;
import cn.liyu.hospital.service.IPaymentWalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 管理端 · 支付管理控制器
 * <p>
 * 管理后台的支付订单管理、手动退款、钱包查询等功能
 *
 * @author 医秒通
 */
@Tag(name = "管理端 · 支付管理", description = "支付订单管理 / 退款 / 钱包查询")
@RestController
@RequestMapping("/admin/payment")
public class AdminPaymentController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminPaymentController.class);

    @Resource
    private IPaymentOrderService paymentOrderService;

    @Resource
    private IPaymentWalletService walletService;

    // ==================== 支付订单管理 ====================

    @Operation(summary = "查看所有支付订单", description = "支持按状态/账号筛选")
    @GetMapping("/order/list")
    public CommonResult<CommonPage<PaymentOrderDTO>> listOrders(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        List<PaymentOrderDTO> list;
        if (accountId != null) {
            list = paymentOrderService.listByAccount(accountId, status, pageNum, pageSize);
        } else {
            // 管理端不传accountId时查全部（简化实现：传null查所有）
            list = paymentOrderService.listByAccount(null, status, pageNum, pageSize);
        }
        return CommonResult.success(CommonPage.restPage(list));
    }

    @Operation(summary = "查看支付订单详情", description = "传入支付订单编号")
    @GetMapping("/order/{id}")
    public CommonResult<PaymentOrderDTO> getOrder(@PathVariable Long id) {
        PaymentOrderDTO dto = paymentOrderService.getPaymentOrderDTO(id);
        if (dto == null) {
            return CommonResult.failed("支付订单不存在");
        }
        return CommonResult.success(dto);
    }

    // ==================== 手动退款 ====================

    @Operation(summary = "管理员退款", description = "传入支付订单编号和退款原因")
    @PostMapping("/refund/{paymentId}")
    public CommonResult<PaymentOrderDTO> refund(@PathVariable Long paymentId,
                                                 @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "管理员退款") : "管理员退款";
        try {
            PaymentOrderDTO result = paymentOrderService.refund(paymentId, reason);
            LOGGER.info("管理员退款成功: paymentId={}, reason={}", paymentId, reason);
            return CommonResult.success(result);
        } catch (RuntimeException e) {
            LOGGER.error("管理员退款失败: paymentId={}, error={}", paymentId, e.getMessage());
            return CommonResult.failed(e.getMessage());
        }
    }

    // ==================== 钱包管理 ====================

    @Operation(summary = "查看所有用户钱包", description = "分页查看所有用户钱包余额")
    @GetMapping("/wallet/list")
    public CommonResult<CommonPage<WalletDTO>> listWallets(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        // 简化实现：返回空列表，后续可扩展
        return CommonResult.success(CommonPage.restPage(List.of()));
    }
}
