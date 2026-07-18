package cn.liyu.hospital.controller.admin;

import cn.liyu.hospital.common.api.CommonResult;
import cn.liyu.hospital.dto.param.HospitalSpecialParam;
import cn.liyu.hospital.service.IHospitalSpecialService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @author 医秒通
 */

@Tag(name = "管理端 · 专科管理", description = "医院专科接口")
@RestController
@RequestMapping("/hospital")
public class HospitalSpecialController {

    @Resource
    private IHospitalSpecialService specialService;

    @Operation(summary = "添加专科信息", description = "传入 专科信息参数（名称，描述）")
    @PostMapping("/special")
    public CommonResult<String> insertSpecial(@RequestBody HospitalSpecialParam param) {

        if (specialService.count(param.getName())) {
            return CommonResult.validateFailed("已存在，该专科名称! ");
        }

        if (specialService.insert(param)) {
            return CommonResult.success();
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "更新专科信息", description = "传入 专科编号、专科信息参数（名称，描述）")
    @PutMapping("/special/{id}")
    public CommonResult<String> updateSpecial(@PathVariable Long id, @RequestBody HospitalSpecialParam param) {

        if (!specialService.count(id)) {
            return CommonResult.validateFailed("不存在，该专科编号! ");
        }

        if (specialService.update(id, param)) {
            return CommonResult.success();
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "删除专科信息", description = "传入 专科编号")
    @DeleteMapping("/special/{id}")
    public CommonResult<String> deleteSpecial(@PathVariable Long id) {

        if (!specialService.count(id)) {
            return CommonResult.validateFailed("不存在，该专科编号! ");
        }

        if (specialService.delete(id)) {
            return CommonResult.success();
        }
        return CommonResult.failed("服务器错误，请联系管理员！");
    }
}
