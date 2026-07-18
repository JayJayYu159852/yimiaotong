package cn.liyu.hospital.controller.admin;

import cn.liyu.hospital.common.api.CommonPage;
import cn.liyu.hospital.common.api.CommonResult;
import cn.liyu.hospital.dto.UserBasicInfoDTO;
import cn.liyu.hospital.entity.PowerAccount;
import cn.liyu.hospital.entity.UserBasicInfo;
import cn.liyu.hospital.entity.UserMedicalCard;
import cn.liyu.hospital.service.IPowerAccountService;
import cn.liyu.hospital.service.IUserBasicInfoService;
import cn.liyu.hospital.service.IUserMedicalCardService;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端 · 用户管理 —— 搜索用户信息 / 搜索就诊卡
 *
 * @author 医秒通
 */

@Tag(name = "管理端 · 用户管理", description = "用户信息与就诊卡搜索接口")
@RestController
@RequestMapping("/user")
public class AdminUserController {

    @Resource
    private IUserBasicInfoService basicInfoService;

    @Resource
    private IUserMedicalCardService medicalCardService;

    @Resource
    private IPowerAccountService powerAccountService;

    @Operation(summary = "分页：搜索用户信息", description = "传入 用户姓名、手机号、第几页、页大小")
    @GetMapping("/basic/list")
    public CommonResult<CommonPage<UserBasicInfoDTO>> searchBasicInfo(@RequestParam(required = false) String name,
                                                                      @RequestParam(required = false) String phone,
                                                                      @RequestParam Integer pageNum,
                                                                      @RequestParam Integer pageSize) {
        // 1. 获取原始用户列表（保留分页信息）
        List<UserBasicInfo> rawList = basicInfoService.list(name, phone, pageNum, pageSize);
        PageInfo<UserBasicInfo> pageInfo = new PageInfo<>(rawList);

        // 2. 批量查询账号状态（按手机号关联 PowerAccount.name）
        List<String> phones = rawList.stream()
                .map(UserBasicInfo::getPhone)
                .filter(p -> p != null && !p.isEmpty())
                .collect(Collectors.toList());
        Map<String, Integer> statusMap = powerAccountService.listByNames(phones).stream()
                .collect(Collectors.toMap(PowerAccount::getName, PowerAccount::getStatus, (a, b) -> a));

        // 3. 构建 DTO
        List<UserBasicInfoDTO> dtoList = rawList.stream().map(u -> {
            UserBasicInfoDTO dto = new UserBasicInfoDTO();
            dto.setId(u.getId());
            dto.setName(u.getName());
            dto.setAvatarUrl(u.getAvatarUrl());
            dto.setPhone(u.getPhone());
            dto.setGmtCreate(u.getGmtCreate());
            dto.setStatus(statusMap.getOrDefault(u.getPhone(), 0));
            return dto;
        }).collect(Collectors.toList());

        // 4. 手工构建 CommonPage，保留真实分页数据
        CommonPage<UserBasicInfoDTO> page = new CommonPage<>();
        page.setPageNum(pageInfo.getPageNum());
        page.setPageSize(pageInfo.getPageSize());
        page.setTotalPage(pageInfo.getPages());
        page.setTotal(pageInfo.getTotal());
        page.setList(dtoList);
        return CommonResult.success(page);
    }

    @Operation(summary = "分页：搜索就诊卡信息", description = "传入 用户姓名、手机号、性别、第几页、页大小")
    @GetMapping("/card/list")
    public CommonResult<CommonPage<UserMedicalCard>> searchMedicalCard(@RequestParam(required = false) String name,
                                                                          @RequestParam(required = false) String phone,
                                                                          @RequestParam(defaultValue = "0") Integer gender,
                                                                          @RequestParam Integer pageNum,
                                                                          @RequestParam Integer pageSize) {
        if (gender < 0 || gender > 2) {
            return CommonResult.validateFailed("性别参数错误：" + gender);
        }
        return CommonResult.success(CommonPage.restPage(medicalCardService.list(name, phone, gender, pageNum, pageSize)));
    }

    @Operation(summary = "删除用户基础信息", description = "传入 用户编号")
    @DeleteMapping("/basic/{id}")
    public CommonResult<String> deleteBasicInfo(@PathVariable Long id) {
        if (!basicInfoService.count(id)) return CommonResult.validateFailed("不存在，该用户编号！");
        return basicInfoService.delete(id) ? CommonResult.success() : CommonResult.failed("删除失败");
    }
}
