package cn.liyu.hospital.controller.admin;

import cn.liyu.hospital.common.api.CommonPage;
import cn.liyu.hospital.common.api.CommonResult;
import cn.liyu.hospital.common.security.DynamicSecurityMetadataSource;
import cn.liyu.hospital.dto.param.PowerResourceParam;
import cn.liyu.hospital.entity.PowerResource;
import cn.liyu.hospital.service.IPowerResourceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

/**
 * @author 医秒通
 */
@Tag(name = "管理端 · 资源管理", description = "权限资源接口")
@RestController
@RequestMapping("/power")
public class PowerResourceController {

    @Resource
    private IPowerResourceService resourceService;

    @Resource
    private DynamicSecurityMetadataSource dynamicSecurityMetadataSource;

    @Operation(summary = "添加权限资源")
    @PostMapping("/resource")
    public CommonResult<String> insertResource(@RequestBody PowerResourceParam param) {

        if (resourceService.insert(param)) {
            dynamicSecurityMetadataSource.clearDataSource();
            return CommonResult.success();
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "修改权限资源")
    @PutMapping("/resource/{id}")
    public CommonResult<String> updateResource(@PathVariable Long id, @RequestBody PowerResourceParam param) {

        if (!resourceService.count(id)) {
            return CommonResult.validateFailed("不存在，该资源编号！");
        }

        if (resourceService.update(id, param)) {
            dynamicSecurityMetadataSource.clearDataSource();
            return CommonResult.success();
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "获取资源详情")
    @GetMapping("/resource/{id}")
    public CommonResult<PowerResource> getResource(@PathVariable Long id) {

        if (!resourceService.count(id)) {
            return CommonResult.validateFailed("不存在，该资源编号！");
        }

        Optional<PowerResource> optionalPowerResource = resourceService.get(id);

        return optionalPowerResource.map(CommonResult::success)
                .orElseGet(() -> CommonResult.failed("服务器错误，请联系管理员！"));

    }

    @Operation(summary = "删除权限资源")
    @DeleteMapping("/resource/{id}")
    public CommonResult<String> deleteResource(@PathVariable Long id) {

        if (!resourceService.count(id)) {
            return CommonResult.validateFailed("不存在，该资源编号！");
        }

        if (resourceService.delete(id)) {
            dynamicSecurityMetadataSource.clearDataSource();
            return CommonResult.success();
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "分页：模糊查询权限资源")
    @GetMapping("/resource/search")
    public CommonResult<CommonPage<PowerResource>> listResource(@RequestParam(required = false) Long categoryId,
                                                                @RequestParam(required = false) String nameKeyword,
                                                                @RequestParam(required = false) String urlKeyword,
                                                                @RequestParam(defaultValue = "5") Integer pageSize,
                                                                @RequestParam(defaultValue = "1") Integer pageNum) {
        List<PowerResource> resourceList = resourceService.list(categoryId, nameKeyword, urlKeyword, pageSize, pageNum);
        return CommonResult.success(CommonPage.restPage(resourceList));
    }

    @Operation(summary = "查询所有权限资源")
    @GetMapping("/resource/list")
    public CommonResult<List<PowerResource>> listAllResource() {
        List<PowerResource> resourceList = resourceService.listAll();
        return CommonResult.success(resourceList);
    }

}
