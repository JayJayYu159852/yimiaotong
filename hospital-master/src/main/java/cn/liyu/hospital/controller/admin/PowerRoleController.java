package cn.liyu.hospital.controller.admin;

import cn.liyu.hospital.common.api.CommonPage;
import cn.liyu.hospital.common.api.CommonResult;
import cn.liyu.hospital.dto.param.PowerRoleParam;
import cn.liyu.hospital.dto.param.StatusParam;
import cn.liyu.hospital.entity.PowerResource;
import cn.liyu.hospital.entity.PowerRole;
import cn.liyu.hospital.service.IPowerRoleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 权限角色接口
 *
 * @author 医秒通
 */

@Tag(name = "管理端 · 角色管理", description = "权限角色接口")
@RestController
@RequestMapping("/power/role")
public class PowerRoleController {

    @Resource
    private IPowerRoleService roleService;

    @Operation(summary = "分页：搜索权限角色", description = "传入 第几页、页大小")
@GetMapping("/list")
    public CommonResult<CommonPage<PowerRole>> searchRole(@RequestParam(required = false) String chineseName,
                                                          @RequestParam Integer pageNum,
                                                          @RequestParam Integer pageSize) {

        return CommonResult.success(CommonPage.restPage(roleService.list(chineseName, pageNum, pageSize)));
    }

    @Operation(summary = "增加权限角色", description = "传入 权限角色参数")
    @PostMapping("")
    public CommonResult<String> insertRole(@RequestBody PowerRoleParam param) {

        if (roleService.insert(param)) {
            return CommonResult.success();
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "修改权限角色", description = "传入 权限角色参数")
    @PutMapping("/{id}")
    public CommonResult<String> updateRole(@PathVariable Long id, @RequestBody PowerRoleParam param) {

        if (!roleService.count(id)) {
            return CommonResult.validateFailed("不存在，该角色编号！");
        }

        if (roleService.update(id, param)) {
            return CommonResult.success();
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "删除权限角色", description = "传入 权限角色编号")
    @DeleteMapping("/{id}")
    public CommonResult<String> deleteRole(@PathVariable Long id) {

        if (!roleService.count(id)) {
            return CommonResult.validateFailed("不存在，该角色编号！");
        }

        if (roleService.delete(id)) {
            return CommonResult.success();
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }


    @Operation(summary = "更新角色状态", description = "传入 权限角色编号、状态参数")
    @PutMapping("/status/{id}")
    public CommonResult<String> updateRoleStatus(@PathVariable Long id, @RequestBody StatusParam param) {

        if (!roleService.count(id)) {
            return CommonResult.validateFailed("不存在，该角色编号！");
        }

        if (roleService.updateStatus(id, param)) {
            return CommonResult.success();
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }


    @Operation(summary = "获取角色相关资源")
    @GetMapping("/resource/{roleId}")
    public CommonResult<List<PowerResource>> listRoleResource(@PathVariable Long roleId) {

        if (!roleService.count(roleId)) {
            return CommonResult.validateFailed("不存在，该角色编号！");
        }

        List<PowerResource> roleList = roleService.listMenuResource(roleId);
        return CommonResult.success(roleList);
    }

    @Operation(summary = "给角色分配资源")
    @PostMapping("/allocResource")
    public CommonResult<String> allocResource(@RequestParam Long roleId, @RequestParam List<Long> resourceIdList) {

        if (!roleService.count(roleId)) {
            return CommonResult.validateFailed("不存在，该角色编号！");
        }


        if (roleService.allocResource(roleId, resourceIdList)) {
            return CommonResult.success();
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }
}
