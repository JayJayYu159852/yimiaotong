package cn.liyu.hospital.controller.admin;

import cn.liyu.hospital.common.api.CommonResult;
import cn.liyu.hospital.dto.param.HospitalDoctorParam;
import cn.liyu.hospital.service.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @author 医秒通
 */

@Tag(name = "管理端 · 医生管理", description = "医生信息接口")
@RestController
@RequestMapping("/hospital")
public class HospitalDoctorController {

    private static final int GIRL = 2;
    private static final int BOY = 1;

    @Resource
    private IHospitalSpecialService specialService;

    @Resource
    private IHospitalDoctorService doctorService;

    @Operation(summary = "添加医生信息")
    @PostMapping("/doctor")
    public CommonResult<String> insertDoctor(@RequestBody HospitalDoctorParam param) {
        if (param.getGender() > GIRL || param.getGender() < BOY) {
            return CommonResult.validateFailed("性别参数错误！");
        }
        if (!specialService.count(param.getSpecialId())) {
            return CommonResult.validateFailed("不存在，该专科编号！");
        }
        if (doctorService.insert(param)) {
            return CommonResult.success();
        }
        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "更新医生信息")
    @PutMapping("/doctor/{id}")
    public CommonResult<String> updateDoctor(@PathVariable Long id, @RequestBody HospitalDoctorParam param) {
        if (!doctorService.count(id)) {
            return CommonResult.validateFailed("不存在，该医生编号");
        }
        if (param.getGender() > GIRL || param.getGender() < BOY) {
            return CommonResult.validateFailed("性别参数错误！");
        }
        if (!specialService.count(param.getSpecialId())) {
            return CommonResult.validateFailed("不存在，该专科编号！");
        }
        if (doctorService.update(id, param)) {
            return CommonResult.success();
        }
        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "删除医生信息")
    @DeleteMapping("/doctor/{id}")
    public CommonResult<String> deleteDoctor(@PathVariable Long id) {
        if (!doctorService.count(id)) {
            return CommonResult.validateFailed("不存在，该医生编号");
        }
        if (doctorService.delete(id)) {
            return CommonResult.success();
        }
        return CommonResult.failed("服务器错误，请联系管理员！");
    }
}
