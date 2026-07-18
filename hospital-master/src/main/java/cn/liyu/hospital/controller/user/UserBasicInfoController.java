package cn.liyu.hospital.controller.user;

import cn.liyu.hospital.common.api.CommonResult;
import cn.liyu.hospital.common.security.JwtTokenUtil;
import cn.liyu.hospital.component.WxComponent;
import cn.liyu.hospital.dto.UserInfoDTO;
import cn.liyu.hospital.dto.param.PowerAccountPasswordParam;
import cn.liyu.hospital.dto.param.UserBasicInfoParam;
import cn.liyu.hospital.dto.param.UserLoginParam;
import cn.liyu.hospital.dto.param.UserRegisterParam;
import cn.liyu.hospital.entity.PowerAccount;
import cn.liyu.hospital.entity.UserBasicInfo;
import cn.liyu.hospital.service.IPowerAccountService;
import cn.liyu.hospital.service.IUserBasicInfoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import cn.hutool.core.util.StrUtil;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.security.Principal;
import java.util.Optional;

/**
 * @author 医秒通
 */

@Tag(name = "用户端 · 基本信息", description = "用户信息接口")
@RestController
@RequestMapping("/user")
public class UserBasicInfoController {

    @Resource
    private IUserBasicInfoService basicInfoService;

    @Resource
    private IPowerAccountService powerAccountService;

    @Resource
    private WxComponent wxComponent;

    @Resource
    private UserDetailsService userDetailsService;

    @Resource
    private JwtTokenUtil jwtTokenUtil;

    @Operation(summary = "发送短信验证码", description = "传入手机号（不管是否注册都发送）")
    @GetMapping("/basic/message")
    public CommonResult<String> sendMessage(@RequestParam String phone) {

        if (StrUtil.isEmpty(phone)) {
            return CommonResult.validateFailed("手机号码不能为空！");
        }

        if (!basicInfoService.isValidPhone(phone)) {
            return CommonResult.validateFailed("手机号格式不正确！");
        }

        if (basicInfoService.sendMessage(phone)) {
            return CommonResult.success();
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "校验短信验证码", description = "传入 手机号、短信验证码")
    @PostMapping("/basic/code")
    public CommonResult<String> verifyCode(@RequestParam String phone, @RequestParam String code) {
        if (basicInfoService.verifyCode(phone, code)) {
            return CommonResult.success("验证通过");
        }
        return CommonResult.validateFailed("短信验证码错误！");
    }

    @Operation(summary = "授权获取 openid", description = "传入 session_code")
    @GetMapping("/wx")
    public CommonResult<String> getWxOpenId(@RequestParam String code) {

        if (StrUtil.isEmpty(code)) {
            return CommonResult.validateFailed("code为空！");
        }

        return CommonResult.success(wxComponent.getOpenId(code));
    }

    @Operation(summary = "用户登录（验证码/密码二选一）",
               description = "验证码登录：无论是否注册均可发送验证码；已注册用户直接登录，未注册用户返回提示引导注册。" +
                             "密码登录：必须已注册。")
    @PostMapping("/basic/login")
    public CommonResult<String> login(@RequestBody UserLoginParam param) {

        if (StrUtil.isEmpty(param.getPhone())) {
            return CommonResult.validateFailed("手机号不能为空！");
        }

        if (StrUtil.isEmpty(param.getCode()) && StrUtil.isEmpty(param.getPassword())) {
            return CommonResult.validateFailed("验证码或密码必须填写一个！");
        }

        // 方式一：验证码登录
        if (StrUtil.isNotEmpty(param.getCode())) {
            if (!basicInfoService.verifyCode(param.getPhone(), param.getCode())) {
                return CommonResult.validateFailed("短信验证码错误！");
            }

            // 用户不存在 → 返回提示让前端引导注册
            if (!powerAccountService.count(param.getPhone())) {
                return CommonResult.failed("该手机号尚未注册，请填写注册信息完成注册");
            }

            // 用户存在，签发 JWT
            String jwt = jwtTokenUtil.generateToken(
                    userDetailsService.loadUserByUsername(param.getPhone()));
            return CommonResult.success(jwt);
        }

        // 方式二：密码登录
        if (!powerAccountService.count(param.getPhone())) {
            return CommonResult.validateFailed("该手机号尚未注册！");
        }
        Optional<String> tokenOpt = powerAccountService.login(param.getPhone(), param.getPassword());
        return tokenOpt.map(CommonResult::success)
                .orElseGet(() -> CommonResult.validateFailed("密码错误！"));
    }

    @Operation(summary = "用户账号注册", description = "传入 注册对象参数（姓名、头像、手机号、密码、验证码）")
    @PostMapping("/basic/account/register")
    public CommonResult<String> registerUserAccount(@RequestBody UserRegisterParam param) {
        if (powerAccountService.count(param.getPhone())) {
            return CommonResult.validateFailed("该手机号已注册！");
        }

        // 注册前必须校验短信验证码
        if (!basicInfoService.verifyCode(param.getPhone(), param.getCode())) {
            return CommonResult.validateFailed("短信验证码错误！");
        }

        if (powerAccountService.registerUser(param)) {
            return CommonResult.success();
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "获取当前用户信息", description = "无需参数，通过 jwt校验")
    @GetMapping("/basic/info")
    public CommonResult<UserInfoDTO> getCurrentUserInfo(Principal principal) {

        if (principal == null) {
            return CommonResult.validateFailed("principal 对象为空！");
        }

        String userName = principal.getName();

        Optional<PowerAccount> optional = powerAccountService.getByName(userName);

        if (optional.isPresent()) {

            PowerAccount account = optional.get();
            account.setPassword(null);

            UserInfoDTO dto = new UserInfoDTO();

            dto.setAccount(account);

            Optional<UserBasicInfo> infoOptional = basicInfoService.getOptionalByPhone(account.getName());

            infoOptional.ifPresent(dto::setBasicInfo);

            return CommonResult.success(dto);
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "更新用户基础信息", description = "传入 用户编号、用户信息参数（姓名、性别、出生日期）")
    @PutMapping("/basic/{id}")
    public CommonResult<String> updateBasicInfo(@PathVariable Long id, @RequestBody UserBasicInfoParam param) {
        if (!basicInfoService.count(id)) {
            return CommonResult.validateFailed("不存在，该用户编号！");
        }

        if (basicInfoService.update(id, param)) {
            return CommonResult.success();
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "更新用户账号密码", description = "传入 账号修改密码对象参数（手机号、密码、验证码）")
    @PutMapping("/basic/password")
    public CommonResult<String> updateBasicInfoPassword(@RequestBody PowerAccountPasswordParam param) {

        if (StrUtil.isEmpty(param.getPassword())) {
            return CommonResult.validateFailed("账号密码不能为空！");
        }

        if (StrUtil.isEmpty(param.getName())) {
            return CommonResult.validateFailed("账号名称不能为空！");
        }

        if (!basicInfoService.verifyCode(param.getName(), param.getCode())) {
            return CommonResult.validateFailed("短信验证码错误！");
        }

        Optional<PowerAccount> accountOptional = powerAccountService.getByName(param.getName());

        if (!accountOptional.isPresent()) {
            return CommonResult.validateFailed("不存在，该用户账号！");
        }

        if (powerAccountService.updatePassword(accountOptional.get().getId(), param.getPassword())) {
            return CommonResult.success();
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "退出登录", description = "前端清除 token 即可，后端无需操作")
    @PostMapping("/basic/logout")
    public CommonResult<String> logout() {
        return CommonResult.success("已退出");
    }
}
