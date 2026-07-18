package cn.liyu.hospital.controller.user;

import cn.hutool.core.date.DateUtil;
import cn.liyu.hospital.common.api.CommonPage;
import cn.liyu.hospital.common.api.CommonResult;
import cn.liyu.hospital.dto.VisitDoctorPlanDTO;
import cn.liyu.hospital.dto.VisitPlanDTO;
import cn.liyu.hospital.dto.VisitPlanResiduesDTO;
import cn.liyu.hospital.service.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 用户端 · 出诊计划 — 浏览医生排班、号源信息
 *
 * @author 医秒通
 */

@Tag(name = "用户端 · 出诊计划", description = "出诊计划查询接口")
@RestController
@RequestMapping("/visit")
public class UserVisitPlanController {

    @Resource
    private IVisitPlanService planService;

    @Resource
    private IHospitalInfoService hospitalInfoService;

    @Resource
    private IHospitalDoctorService hospitalDoctorService;

    @Resource
    private IHospitalSpecialService hospitalSpecialService;

    @Operation(summary = "搜索出诊计划", description = "传入 医院编号、专科编号、医生编号、出诊日期、第几页、页大小")
    @GetMapping("/plan/list")
    public CommonResult<CommonPage<VisitPlanDTO>> searchPlan(@RequestParam(required = false) Long hospitalId,
                                                              @RequestParam(required = false) Long specialId,
                                                              @RequestParam(required = false) Long doctorId,
                                                              @RequestParam String day,
                                                              @RequestParam Integer pageNum,
                                                              @RequestParam Integer pageSize) {
        return CommonResult.success(CommonPage.restPage(
                planService.list(hospitalId, specialId, doctorId,
                        DateUtil.parse(day), pageNum, pageSize)));
    }

    @Operation(summary = "根据医生获取出诊信息", description = "传入 医生编号、开始日期、结束日期")
    @GetMapping("/plan/doctor")
    public CommonResult<VisitDoctorPlanDTO> searchPlanByDoctor(@RequestParam Long doctorId,
                                                                @RequestParam String startDate,
                                                                @RequestParam String endDate) {
        if (!hospitalDoctorService.count(doctorId)) {
            return CommonResult.validateFailed("不存在，该医生编号！");
        }

        Date start = DateUtil.parseDate(startDate);
        Date end = DateUtil.parseDate(endDate);

        if (start.getTime() > end.getTime()) {
            return CommonResult.validateFailed("日期范围不正确！");
        }

        return CommonResult.success(planService.getDoctorPlan(doctorId, start, end));
    }

    @Operation(summary = "根据医生编号、日期获取号源", description = "传入 医院编号、医生编号、日期")
    @GetMapping("/plan/doctor/date")
    public CommonResult<List<VisitPlanResiduesDTO>> searchPlanByDoctorAndDate(@RequestParam Long hospitalId,
                                                                               @RequestParam Long doctorId,
                                                                               @RequestParam String date) {
        if (!hospitalInfoService.count(hospitalId)) {
            return CommonResult.validateFailed("不存在，该医院编号！");
        }

        if (!hospitalDoctorService.count(doctorId)) {
            return CommonResult.validateFailed("不存在，该医生编号！");
        }

        Date time = DateUtil.parseDate(date);
        return CommonResult.success(planService.getDoctorPlanByDate(hospitalId, doctorId, time));
    }
}
