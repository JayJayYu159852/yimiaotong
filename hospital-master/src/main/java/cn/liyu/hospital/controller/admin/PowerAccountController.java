package cn.liyu.hospital.controller.admin;

import cn.liyu.hospital.common.api.CommonResult;
import cn.liyu.hospital.dto.param.PowerAccountRegisterParam;
import cn.liyu.hospital.dto.param.PowerAccountStatusParam;
import cn.liyu.hospital.dto.param.PowerAccountUpdatePasswordParam;
import cn.liyu.hospital.entity.PowerAccount;
import cn.liyu.hospital.service.IPowerAccountService;
import cn.liyu.hospital.service.IPowerRoleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.Operation;
import cn.hutool.core.util.StrUtil;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 权限账号接口
 *
 * @author 医秒通
 */

@Tag(name = "管理端 · 账号管理", description = "权限账号接口")
@RestController
@RequestMapping("/power/account")
public class PowerAccountController {

    @Resource
    private IPowerAccountService accountService;

    @Resource
    private IPowerRoleService roleService;

    @Operation(summary = "验证账号名称", description = "传入 账号名称, 返回是否存在")
    @GetMapping("/name/validation")
    public CommonResult<Boolean> verifyAccountName(@RequestParam String name) {
        return CommonResult.success(accountService.count(name));
    }

    @Operation(summary = "管理账号注册", description = "传入 注册对象参数（账号名称、密码）")
    @PostMapping("/admin/register")
    public CommonResult<String> registerAccount(@RequestBody PowerAccountRegisterParam param) {
        if (accountService.count(param.getName())) {
            return CommonResult.validateFailed("该账号名称已存在！");
        }

        if (accountService.registerAdmin(param)) {
            return CommonResult.success();
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "账号登录", description = "传入 账号名称、账号密码")
@PostMapping("/login")
    public CommonResult<String> loginAccount(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String password = body.get("password");
        if (name == null || password == null) {
            return CommonResult.validateFailed("参数不完整");
        }
        if (!accountService.count(name)) {
            return CommonResult.validateFailed("不存在，该账号名称！");
        }

        Optional<String> stringOptional = accountService.login(name, password);

        return stringOptional.map(CommonResult::success).orElseGet(() -> CommonResult.validateFailed("用户名或密码错误"));

    }

    @Operation(summary = "退出登录账号", description = "无需参数，需要前端清除 header中的 jwt字符串")
    @GetMapping("/logout")
    public CommonResult<String> logoutAccount() {
        return CommonResult.success();
    }

    @Operation(summary = "获取当前账号信息", description = "无需参数，通过 jwt校验")
    @GetMapping("/info")
    public CommonResult<Map<String, Object>> getCurrentAccount(Principal principal) {
        if (principal == null) {
            return CommonResult.unauthorized(null);
        }

        String name = principal.getName();

        Optional<PowerAccount> accountOptional = accountService.getByName(name);

        if (accountOptional.isPresent()) {

            Map<String, Object> info = new HashMap<>();

            PowerAccount account = accountOptional.get();
            account.setPassword(null);

            info.put("accountId", account.getId());
            info.put("userName", account.getName());
            // 返回用户角色列表
            info.put("roles", new String[]{"admin"});
            info.put("menus", new java.util.ArrayList<>());

            return CommonResult.success(info);
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "更新账号密码", description = "传入 账号名称、旧密码、新密码")
    @PutMapping("/password")
    public CommonResult<String> updateAccountPassword(@RequestBody PowerAccountUpdatePasswordParam param) {

        if (!accountService.count(param.getAccountId())) {
            return CommonResult.validateFailed("不存在，该账号编号！");
        }

        if (!accountService.checkPassword(param.getAccountId(), param.getPassword())) {
            return CommonResult.validateFailed("原密码不正确！");
        }

        if (accountService.updatePassword(param.getAccountId(), param.getNewPassword())) {
            return CommonResult.success();
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "更新账号状态", description = "传入 账号编号、账号状态（1：开启，0：关闭）")
    @PutMapping("/status")
    public CommonResult<String> updateAccountStatus(@RequestBody PowerAccountStatusParam param) {

        if (param.getStatus() > 1 || param.getStatus() < 0) {
            return CommonResult.validateFailed("不存在，该账号状态！");
        }

        if (!accountService.count(param.getAccountId())) {
            return CommonResult.validateFailed("不存在，该账号编号！");
        }

        if (accountService.updateStatus(param)) {
            return CommonResult.success();
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "更新账号分配角色", description = "传入 账号编号，角色编号列表")
    @PostMapping("/role")
    public CommonResult<Integer> updateAccountRoleRelation(@RequestParam Long accountId,
                                                           @RequestBody List<Long> roleIdList) {
        if (!accountService.count(accountId)) {
            return CommonResult.validateFailed("不存在，该账号编号！");
        }

        int count = accountService.updateRole(accountId, roleIdList);

        if (count >= 0) {
            return CommonResult.success(count);
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }

}
