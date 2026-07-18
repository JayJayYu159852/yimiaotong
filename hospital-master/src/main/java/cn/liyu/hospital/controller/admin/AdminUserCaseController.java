package cn.liyu.hospital.controller.admin;

import cn.liyu.hospital.common.api.CommonPage;
import cn.liyu.hospital.common.api.CommonResult;
import cn.liyu.hospital.dto.param.UserCaseParam;
import cn.liyu.hospital.dto.param.UserCaseUpdateParam;
import cn.liyu.hospital.entity.UserCase;
import cn.liyu.hospital.entity.VisitAppointment;
import cn.liyu.hospital.service.IHospitalDoctorService;
import cn.liyu.hospital.service.IUserCaseService;
import cn.liyu.hospital.service.IUserMedicalCardService;
import cn.liyu.hospital.service.IVisitAppointmentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Optional;

import static cn.liyu.hospital.dto.AppointmentEnum.FINISH;

/**
 * 管理端 · 病例管理（权限由 DynamicSecurityFilter 统一管理）
 *
 * @author 医秒通
 */

@Tag(name = "管理端 · 病例管理", description = "用户病例接口（医生/管理员专用）")
@RestController
@RequestMapping("/user")
public class AdminUserCaseController {

    @Resource
    private IUserCaseService caseService;

    @Resource
    private IUserMedicalCardService cardService;

    @Resource
    private IHospitalDoctorService hospitalDoctorService;

    @Resource
    private IVisitAppointmentService appointmentService;

    @Operation(summary = "添加用户病例", description = "传入 用户病例参数（就诊卡编号、预约编号、医生编号、病例详情）")
    @PostMapping("/case")
    public CommonResult<String> insertUserCase(@RequestBody UserCaseParam param) {

        if (!cardService.countCardId(param.getCardId())) {
            return CommonResult.validateFailed("不存在，该就诊卡编号！");
        }

        if (!hospitalDoctorService.count(param.getDoctorId())) {
            return CommonResult.validateFailed("不存在，该医生编号！");
        }

        // 预约必须真实存在（防止给编造的预约号建病历）
        Optional<VisitAppointment> appointmentOpt = appointmentService.getOptional(param.getAppointmentId());
        if (!appointmentOpt.isPresent()) {
            return CommonResult.validateFailed("不存在，该预约编号！");
        }

        // 仅已完成的预约才能填写病历（与状态机呼应：先标记完成，再建病历）
        if (!FINISH.getStatus().equals(appointmentOpt.get().getStatus())) {
            return CommonResult.validateFailed("仅已完成的预约才能填写病历！");
        }

        // 防重复建档：一次预约只能有一份病历
        if (caseService.get(param.getCardId(), param.getAppointmentId()) != null) {
            return CommonResult.validateFailed("该预约已有病历，请勿重复创建！");
        }

        if (caseService.insert(param)) {
            return CommonResult.success();
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "更新用户病例", description = "传入 病例编号、用户病例更新参数（病例详情）")
    @PutMapping("/case/{id}")
    public CommonResult<String> updateUserCase(@PathVariable Long id, @RequestBody UserCaseUpdateParam param) {
        if (!caseService.count(id)) {
            return CommonResult.validateFailed("不存在，该病例编号！");
        }

        if (caseService.update(id, param)) {
            return CommonResult.success();
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "删除用户病例", description = "传入 病例编号")
    @DeleteMapping("/case/{id}")
    public CommonResult<String> deleteUserCase(@PathVariable Long id) {

        if (!caseService.count(id)) {
            return CommonResult.validateFailed("不存在，该病例编号！");
        }

        if (caseService.delete(id)) {
            return CommonResult.success();
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "查找用户病例列表", description = "传入 就诊卡编号、第几页、页大小")
    @GetMapping("/case/list")
    public CommonResult<CommonPage<UserCase>> listUserCase(@RequestParam(required = false) Long cardId,
                                                           @RequestParam Integer pageNum, @RequestParam Integer pageSize) {
        return CommonResult.success(CommonPage.restPage(caseService.list(cardId, pageNum, pageSize)));
    }
}
