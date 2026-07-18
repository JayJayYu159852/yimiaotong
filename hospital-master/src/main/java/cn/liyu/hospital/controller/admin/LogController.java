package cn.liyu.hospital.controller.admin;

import cn.liyu.hospital.common.api.CommonPage;
import cn.liyu.hospital.common.api.CommonResult;
import cn.liyu.hospital.entity.LogAccountLogin;
import cn.liyu.hospital.entity.LogOperation;
import cn.liyu.hospital.service.ILogAccountLoginService;
import cn.liyu.hospital.service.ILogOperationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 日志接口
 *
 * @author 医秒通
 */

@Tag(name = "管理端 · 系统日志", description = "日志接口")
@RestController
@RequestMapping("/log")
public class LogController {

    @Resource
    private ILogAccountLoginService accountLoginService;

    @Resource
    private ILogOperationService operationService;

    @Operation(summary = "分页：搜索账号登录日志", description = "传入 账号名称")
@GetMapping("/account/login/list")
    public CommonResult<CommonPage<LogAccountLogin>> searchAccountLogin(@RequestParam(required = false) String accountName,
                                                                        @RequestParam Integer pageNum,
                                                                        @RequestParam Integer pageSize) {

        return CommonResult.success(CommonPage.restPage(accountLoginService.search(accountName, pageNum, pageSize)));

    }

    @Operation(summary = "分页：搜索账号操作日志", description = "传入 账号名称，请求方法（get、post、delete、put）")
@GetMapping("/operation/list")
    public CommonResult<CommonPage<LogOperation>> searchOperation(@RequestParam(required = false) String accountName,
                                                                  @RequestParam(required = false) String method,
                                                                  @RequestParam Integer pageNum,
                                                                  @RequestParam Integer pageSize) {

        return CommonResult.success(CommonPage.restPage(operationService.search(accountName, method, pageNum, pageSize)));
    }


}
