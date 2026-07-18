package cn.liyu.hospital.controller.admin;

import cn.hutool.core.date.DateUtil;
import cn.liyu.hospital.common.api.CommonPage;
import cn.liyu.hospital.common.api.CommonResult;
import cn.liyu.hospital.dto.AdminAppointmentDTO;
import cn.liyu.hospital.dto.VisitUserInfoDTO;
import cn.liyu.hospital.entity.VisitAppointment;
import cn.liyu.hospital.service.IHospitalDoctorService;
import cn.liyu.hospital.service.IVisitAppointmentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Optional;

import static cn.liyu.hospital.dto.AppointmentEnum.FINISH;
import static cn.liyu.hospital.dto.AppointmentEnum.MISSING;
import static cn.liyu.hospital.dto.AppointmentEnum.WAITING;

/**
 * 管理端 · 预约管理 —— 医生/管理员操作
 *
 * @author 医秒通
 */

@Tag(name = "管理端 · 预约管理", description = "预约状态管理接口")
@RestController
@RequestMapping("/visit")
public class AdminVisitAppointmentController {

    @Resource
    private IVisitAppointmentService appointmentService;

    @Resource
    private IHospitalDoctorService hospitalDoctorService;

    @Operation(summary = "标记预约为迟到", description = "传入 预约编号")
    @PutMapping("/appointment/miss/{id}")
    public CommonResult<String> missAppointment(@PathVariable Long id) {
        return updateAppointmentStatus(id, MISSING.getStatus());
    }

    @Operation(summary = "标记预约为完成", description = "传入 预约编号")
    @PutMapping("/appointment/finish/{id}")
    public CommonResult<String> finishAppointment(@PathVariable Long id) {
        return updateAppointmentStatus(id, FINISH.getStatus());
    }

    @Operation(summary = "预约列表（管理端）", description = "分页查询全部病人的预约，可按就诊卡/医生/日期/状态筛选，聚合患者姓名与出诊计划信息")
    @GetMapping("/appointment/admin/list")
    public CommonResult<CommonPage<AdminAppointmentDTO>> listAdminAppointments(@RequestParam(required = false) Long cardId,
                                                                               @RequestParam(required = false) Long doctorId,
                                                                               @RequestParam(required = false) String date,
                                                                               @RequestParam(required = false) Integer status,
                                                                               @RequestParam Integer pageNum,
                                                                               @RequestParam Integer pageSize) {
        Date day = null;
        if (date != null && !date.isEmpty()) {
            try {
                day = DateUtil.parse(date);
            } catch (Exception e) {
                return CommonResult.validateFailed("日期格式错误，请使用 yyyy-MM-dd 格式");
            }
        }
        return CommonResult.success(CommonPage.restPage(
                appointmentService.listAdminAppointments(cardId, doctorId, day, status, pageNum, pageSize)));
    }

    @Operation(summary = "查看病人列表", description = "可选传入医生编号、日期、时间段进行筛选")
    @GetMapping("/appointment/user")
    public CommonResult<CommonPage<VisitUserInfoDTO>> listVisitUserInfo(@RequestParam(required = false) Long doctorId,
                                                                        @RequestParam(required = false) String date,
                                                                        @RequestParam(required = false) Integer time,
                                                                        @RequestParam Integer pageNum,
                                                                        @RequestParam Integer pageSize) {
        Date day = null;
        if (date != null && !date.isEmpty()) {
            try {
                day = DateUtil.parse(date);
            } catch (Exception e) {
                return CommonResult.validateFailed("日期格式错误，请使用 yyyy-MM-dd 格式");
            }
        }
        return CommonResult.success(CommonPage.restPage(
                appointmentService.listVisitUserInfo(doctorId, time, day, pageNum, pageSize)));
    }

    private CommonResult<String> updateAppointmentStatus(Long id, Integer status) {
        Optional<VisitAppointment> appointmentOpt = appointmentService.getOptional(id);
        if (!appointmentOpt.isPresent()) {
            return CommonResult.validateFailed("不存在，该预约编号！");
        }
        // 状态机校验：仅"待就诊"可标记为 完成/失约
        if (!WAITING.getStatus().equals(appointmentOpt.get().getStatus())) {
            return CommonResult.validateFailed("当前预约状态不允许此操作（仅待就诊的预约可标记）！");
        }
        if (appointmentService.update(id, status)) {
            return CommonResult.success();
        }
        return CommonResult.failed("服务器错误，请联系管理员！");
    }
}
