package cn.liyu.hospital.controller.admin;

import cn.liyu.hospital.common.api.CommonResult;
import cn.liyu.hospital.component.FileStorageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import cn.hutool.core.util.StrUtil;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

/**
 * @author 医秒通
 */
@Tag(name = "管理端 · 文件管理", description = "图片接口")
@RestController
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private FileStorageService fileStorageService;

    @Operation(summary = "上传图片，返回图片url", description = "传入 图片文件")
    @PostMapping("/upload")
    public CommonResult<String> uploadPicture(@RequestParam MultipartFile file) {

        if (file.isEmpty()) {
            return CommonResult.validateFailed("上传图片为空！");
        }

        String url = fileStorageService.uploadFile(file);

        if (StrUtil.isNotEmpty(url)) {
            return CommonResult.success(url);
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }
}
