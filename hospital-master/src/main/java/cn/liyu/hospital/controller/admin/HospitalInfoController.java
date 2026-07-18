package cn.liyu.hospital.controller.admin;

import cn.liyu.hospital.common.api.CommonResult;
import cn.liyu.hospital.dto.param.HospitalInfoParam;
import cn.liyu.hospital.dto.param.HospitalSpecialRelationParam;
import cn.liyu.hospital.service.IHospitalInfoService;
import cn.liyu.hospital.service.IHospitalSpecialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 管理端 · 医院信息 — 增删改操作
 * <p>
 * 查询接口已迁移至用户端 HospitalQueryController
 *
 * @author 医秒通
 */

@Tag(name = "管理端 · 医院信息", description = "医院信息接口")
@RestController
@RequestMapping("/hospital")
public class HospitalInfoController {

    private static final String GEO_KEY = "hospital:geo";

    @Resource
    private IHospitalInfoService infoService;

    @Resource
    private IHospitalSpecialService specialService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Operation(summary = "添加医院信息")
    @PostMapping("/info")
    public CommonResult<String> insertHospitalInfo(@RequestBody HospitalInfoParam param) {
        if (infoService.count(param.getPhone())) {
            return CommonResult.validateFailed("该电话号码，已存在！");
        }
        Long hospitalId = infoService.insert(param);
        if (hospitalId != null) {
            if (param.getLatitude() != null && param.getLongitude() != null) {
                stringRedisTemplate.opsForGeo().add(GEO_KEY,
                        new Point(param.getLongitude(), param.getLatitude()),
                        String.valueOf(hospitalId));
            }
            return CommonResult.success();
        }
        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "更新医院信息")
    @PutMapping("/info/{id}")
    public CommonResult<String> updateHospitalInfo(@PathVariable Long id, @RequestBody HospitalInfoParam param) {
        if (!infoService.count(id)) {
            return CommonResult.validateFailed("不存在，该医院编号！");
        }
        // 检查手机号是否被其他医院占用（排除自己）
        if (infoService.count(param.getPhone())) {
            java.util.Optional<cn.liyu.hospital.entity.HospitalInfo> existing = infoService.getOptional(id);
            if (existing.isPresent() && !existing.get().getPhone().equals(param.getPhone())) {
                return CommonResult.validateFailed("该电话号码已被其他医院使用！");
            }
        }
        if (infoService.update(id, param)) {
            return CommonResult.success();
        }
        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "删除医院信息")
    @DeleteMapping("/info/{id}")
    public CommonResult<String> deleteHospitalInfo(@PathVariable Long id) {
        if (!infoService.count(id)) {
            return CommonResult.validateFailed("不存在，该医院编号！");
        }
        if (infoService.delete(id)) {
            return CommonResult.success();
        }
        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "添加专科到医院中")
    @PostMapping("/special/relation")
    public CommonResult<String> insertSpecialRelation(@RequestBody HospitalSpecialRelationParam param) {
        if (!infoService.count(param.getHospitalId())) {
            return CommonResult.validateFailed("不存在，该医院编号！");
        }
        if (!specialService.count(param.getSpecialId())) {
            return CommonResult.validateFailed("不存在，该专科编号! ");
        }
        if (infoService.countSpecialRelation(param)) {
            return CommonResult.validateFailed("已存在，该专科关系！");
        }
        if (infoService.insertSpecialRelation(param)) {
            return CommonResult.success();
        }
        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "移除医院中的专科")
    @DeleteMapping("/special/relation")
    public CommonResult<String> deleteSpecialRelation(@RequestParam Long hospitalId, @RequestParam Long specialId) {
        if (!infoService.count(hospitalId)) {
            return CommonResult.validateFailed("不存在，该医院编号！");
        }
        if (!specialService.count(specialId)) {
            return CommonResult.validateFailed("不存在，该专科编号！");
        }
        if (infoService.deleteSpecialRelation(hospitalId, specialId)) {
            return CommonResult.success();
        }
        return CommonResult.failed("服务器错误，请联系管理员！");
    }

}
