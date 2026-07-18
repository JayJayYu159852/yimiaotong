package cn.liyu.hospital.controller.admin;

import cn.liyu.hospital.common.api.CommonPage;
import cn.liyu.hospital.common.api.CommonResult;
import cn.liyu.hospital.dto.UserCreditDTO;
import cn.liyu.hospital.entity.VisitAppointment;
import cn.liyu.hospital.service.IPowerAccountService;
import cn.liyu.hospital.service.IUserMedicalCardService;
import cn.liyu.hospital.service.IVisitAppointmentService;
import cn.liyu.hospital.service.IVisitBlacklistService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @author 医秒通
 */

@Tag(name = "管理端 · 预约黑名单", description = "爽约记录与黑名单管理")
@RestController
@RequestMapping("/visit")
public class VisitCreditController {

    @Resource
    private IVisitBlacklistService blacklistService;

    @Resource
    private IUserMedicalCardService userMedicalCardService;

    @Resource
    private IPowerAccountService accountService;

    @Resource
    private IVisitAppointmentService appointmentService;

    @Operation(summary = "验证就诊卡号是否黑名单", description = "传入 就诊卡号")
    @GetMapping("/black/verify")
    public CommonResult<Boolean> verifyBlack(@RequestParam Long cardId) {
        if (!userMedicalCardService.countCardId(cardId)) {
            return CommonResult.validateFailed("不存在，该就诊卡号！");
        }

        // 自动解绑黑名单
        blacklistService.autoUnlock();

        return CommonResult.success(blacklistService.isForbid(cardId));
    }

    @Operation(summary = "获取当月信用详情", description = "传入 账号编号、就诊卡编号")
    @GetMapping("/credit/current")
    public CommonResult<UserCreditDTO> getCurrentCredit(@RequestParam Long accountId, @RequestParam Long cardId) {

        if (!userMedicalCardService.countCardId(cardId)) {
            return CommonResult.validateFailed("不存在，该就诊卡编号！");
        }

        if (!accountService.count(accountId)) {
            return CommonResult.validateFailed("不存在，该账号编号！");
        }

        return CommonResult.success(appointmentService.getCurrentCredit(accountId, cardId));
    }

    @Operation(summary = "获取以往信用详情", description = "传入 账号编号、就诊卡编号")
    @GetMapping("/credit/all")
    public CommonResult<UserCreditDTO> getAllCredit(@RequestParam Long accountId, @RequestParam Long cardId) {

        if (!userMedicalCardService.countCardId(cardId)) {
            return CommonResult.validateFailed("不存在，该就诊卡编号！");
        }

        if (!accountService.count(accountId)) {
            return CommonResult.validateFailed("不存在，该账号编号！");
        }

        return CommonResult.success(appointmentService.getAllCredit(accountId, cardId));
    }

    @Operation(summary = "获取失信记录", description = "传入就诊卡编号")
    @GetMapping("/credit/miss")
    public CommonResult<CommonPage<VisitAppointment>> listMissRecord(@RequestParam Long cardId,
                                                                     @RequestParam Integer pageNum,
                                                                     @RequestParam Integer pageSize) {

        
        return CommonResult.success(CommonPage.restPage(appointmentService.listMiss(cardId, pageNum, pageSize)));

    }
}
