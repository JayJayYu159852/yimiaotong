package cn.liyu.hospital.controller.admin;

import cn.hutool.core.collection.CollUtil;
import cn.liyu.hospital.common.api.CommonPage;
import cn.liyu.hospital.common.api.CommonResult;
import cn.liyu.hospital.dto.VisitPlanDTO;
import cn.liyu.hospital.dto.param.VisitPlanParam;
import cn.liyu.hospital.dto.param.VisitPlanUpdateParam;
import cn.liyu.hospital.service.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

/**
 * @author 医秒通
 */

@Tag(name = "管理端 · 出诊计划", description = "出诊计划接口")
@RestController
@RequestMapping("/visit")
public class VisitPlanController {

    @Resource
    private IVisitPlanService planService;

    @Resource
    private IHospitalInfoService hospitalInfoService;

    @Operation(summary = "分页查询所有出诊计划（管理端使用）")
    @GetMapping("/plan/admin/list")
    public CommonResult<CommonPage<VisitPlanDTO>> listAllVisitPlan(@RequestParam(defaultValue = "1") Integer pageNum,
                                                                    @RequestParam(defaultValue = "10") Integer pageSize) {
        List<VisitPlanDTO> list = planService.listAll(pageNum, pageSize);
        return CommonResult.success(CommonPage.restPage(list));
    }

    @Resource
    private IHospitalDoctorService hospitalDoctorService;

    @Resource
    private IHospitalSpecialService hospitalSpecialService;


    @Operation(summary = "添加出诊计划", description = "传入 出诊计划参数（医院编号、专科编号、诊室编号、医生编号、出诊时间段（1：上午，2：下午）、出诊日期）")
    @PostMapping("/plan")
    public CommonResult<String> insertVisitPlan(@RequestBody VisitPlanParam param) {

        if (!hospitalDoctorService.count(param.getDoctorId())) {
            return CommonResult.validateFailed("不存在，该医生编号！");
        }

        if (!hospitalInfoService.count(param.getHospitalId())) {
            return CommonResult.validateFailed("不存在，该医院编号！");
        }

        if (!hospitalSpecialService.count(param.getSpecialId())) {
            return CommonResult.validateFailed("不存在，该专科编号！");
        }

        if (param.getTime() > 2 || param.getTime() < 1) {
            return CommonResult.validateFailed("不存在，该出诊时间段（1：上午，2：下午）！");
        }

        if (planService.insertAll(param)) {
            return CommonResult.success();
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "更新出诊计划", description = "传入 出诊编号、出诊计划参数（医院编号、专科编号、诊室编号、医生编号、出诊日期）")
    @PutMapping("/plan/{id}")
    public CommonResult<String> updateVisitPlan(@PathVariable Long id, @RequestBody VisitPlanUpdateParam param) {

        if (!hospitalDoctorService.count(param.getDoctorId())) {
            return CommonResult.validateFailed("不存在，该医生编号！");
        }

        if (!hospitalInfoService.count(param.getHospitalId())) {
            return CommonResult.validateFailed("不存在，该医院编号！");
        }

        if (!hospitalSpecialService.count(param.getSpecialId())) {
            return CommonResult.validateFailed("不存在，该专科编号！");
        }

        if (planService.update(id, param)) {
            return CommonResult.success();
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "删除出诊计划", description = "传入 出诊编号")
    @DeleteMapping("/plan/{id}")
    public CommonResult<String> deleteVisitPlan(@PathVariable Long id) {

        if (!planService.count(id)) {
            return CommonResult.validateFailed("不存在，该出诊编号");
        }

        if (planService.delete(id)) {
            return CommonResult.success();
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "批量删除出诊计划", description = "传入 出诊编号列表")
    @DeleteMapping("/plan/all")
    public CommonResult<String> deleteAllVisitPlan(@RequestParam List<Long> idList) {

        if (CollUtil.isEmpty(idList)) {
            return CommonResult.validateFailed("出诊编号列表为空！");
        }

        if (planService.deleteAll(idList)) {
            return CommonResult.success();
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "获取出诊计划详情", description = "传入 出诊编号")
    @GetMapping("/plan/{id}")
    public CommonResult<VisitPlanDTO> getVisitPlan(@PathVariable Long id) {
        if (!planService.count(id)) {
            return CommonResult.validateFailed("不存在，该出诊编号");
        }
        Optional<VisitPlanDTO> opt = planService.getOptional(id);
        return opt.map(CommonResult::success).orElseGet(() -> CommonResult.failed("获取计划详情失败"));
    }

}
