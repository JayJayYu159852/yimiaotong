package cn.liyu.hospital.controller.admin;

import cn.liyu.hospital.common.api.CommonResult;
import cn.liyu.hospital.dto.param.PowerResourceCategoryParam;
import cn.liyu.hospital.entity.PowerResourceCategory;
import cn.liyu.hospital.service.IPowerResourceCategoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author 医秒通
 */
@Tag(name = "管理端 · 资源分类", description = "权限资源分类接口")
@RestController
@RequestMapping("/power")
public class PowerResourceCategoryController {

    @Resource
    private IPowerResourceCategoryService resourceCategoryService;

    @Operation(summary = "查询所有权限资源分类")
    @GetMapping("/category/list")
    public CommonResult<List<PowerResourceCategory>> listAllCategory() {

        List<PowerResourceCategory> resourceList = resourceCategoryService.listAll();

        return CommonResult.success(resourceList);
    }

    @Operation(summary = "添加权限资源分类")
    @PostMapping("/category")
    public CommonResult<String> insertCategory(@RequestBody PowerResourceCategoryParam param) {

        if (resourceCategoryService.insert(param)) {
            return CommonResult.success();
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "修改权限资源分类")
    @PutMapping("/category/{id}")
    public CommonResult<String> updateCategory(@PathVariable Long id, @RequestBody PowerResourceCategoryParam param) {

        if (!resourceCategoryService.count(id)) {
            return CommonResult.validateFailed("不存在，该分类编号！");
        }

        if (resourceCategoryService.update(id, param)) {
            return CommonResult.success();
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "删除权限资源")
    @DeleteMapping("/category/{id}")
    public CommonResult<String> deleteCategory(@PathVariable Long id) {

        if (!resourceCategoryService.count(id)) {
            return CommonResult.validateFailed("不存在，该分类编号！");
        }

        if (resourceCategoryService.delete(id)) {
            return CommonResult.success();
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }
}
