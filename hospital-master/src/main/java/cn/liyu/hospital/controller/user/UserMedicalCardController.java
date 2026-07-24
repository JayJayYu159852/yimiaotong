package cn.liyu.hospital.controller.user;

import cn.liyu.hospital.common.api.CommonResult;
import cn.liyu.hospital.dto.UserMedicalCardDTO;
import cn.liyu.hospital.dto.param.UserMedicalCardParam;
import cn.liyu.hospital.dto.param.UserMedicalCardUpdateParam;
import cn.liyu.hospital.entity.PowerAccount;
import cn.liyu.hospital.entity.UserBasicInfo;
import cn.liyu.hospital.entity.UserMedicalCard;
import cn.liyu.hospital.mapper.PowerAccountMapper;
import cn.liyu.hospital.service.IPowerAccountService;
import cn.liyu.hospital.service.IUserBasicInfoService;
import cn.liyu.hospital.service.IUserMedicalCardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import cn.hutool.core.util.StrUtil;
import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

/**
 * @author 医秒通
 */

@Tag(name = "用户端 · 就诊卡", description = "就诊卡信息接口")
@RestController
@RequestMapping("/user")
public class UserMedicalCardController {

    private static final int MAX_CARD_NUMBER = 6;

    @Resource
    private IUserMedicalCardService medicalCardService;

    @Resource
    private IPowerAccountService powerAccountService;

    @Resource
    private IUserBasicInfoService userBasicInfoService;

    @Resource
    private PowerAccountMapper powerAccountMapper;

    private static final Logger LOGGER = LoggerFactory.getLogger(UserMedicalCardController.class);

    @Operation(summary = "添加就诊卡", description = "传入 账号编号、就诊卡信息参数（关系类型、性别、姓名、手机号、证件号、出生日期）")
    @PostMapping("/card/{accountId}")
    public CommonResult<String> insertMedicalCard(@PathVariable Long accountId, @RequestBody UserMedicalCardParam param) {

        if (!powerAccountService.count(accountId)) {
            return CommonResult.validateFailed("不存在，该账号编号！");
        }

        if (medicalCardService.count(accountId) > MAX_CARD_NUMBER) {
            return CommonResult.validateFailed("绑定就诊卡数量不可超过：" + MAX_CARD_NUMBER + "！");
        }

        if (medicalCardService.insert(accountId, param)) {
            return CommonResult.success();
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }


    @Operation(summary = "修改就诊卡", description = "传入 关系编号、就诊卡更新信息参数（关系类型、性别、姓名、就诊卡编号）")
    @PutMapping("/card/{relationId}")
    public CommonResult<String> updateMedicalCard(@PathVariable Long relationId, @RequestBody UserMedicalCardUpdateParam param) {
        if (!medicalCardService.countRelation(relationId)) {
            return CommonResult.validateFailed("不存在，该关系编号！");
        }

        // 前端可能不传身份证号，有传才需要校验
        if (StrUtil.isNotBlank(param.getIdentificationNumber())) {
            // 查出当前就诊卡 ID，检查身份证号是否被其他卡占用
            UserMedicalCard current = medicalCardService.getOptional(
                    medicalCardService.getCardIdByRelationId(relationId)
            ).orElse(null);
            if (current != null && !param.getIdentificationNumber().equals(current.getIdentificationNumber())) {
                if (medicalCardService.countIdentificationNumberExclude(param.getIdentificationNumber(), current.getId())) {
                    return CommonResult.validateFailed("该身份证号已被其他就诊卡使用！");
                }
            }
        }

        if (medicalCardService.update(relationId, param)) {
            return CommonResult.success();
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "获取就诊卡", description = "传入 就诊卡编号")
    @GetMapping("/card/{cardId}")
    public CommonResult<UserMedicalCard> updateMedicalCard(@PathVariable Long cardId) {
        if (!medicalCardService.countCardId(cardId)) {
            return CommonResult.validateFailed("不存在，该就诊卡编号！");
        }

        Optional<UserMedicalCard> cardOptional = medicalCardService.getOptional(cardId);

        if (cardOptional.isPresent()) {
            return CommonResult.success(cardOptional.get());
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }

    @Operation(summary = "获取用户就诊卡", description = "传入 账号编号。若用户尚无就诊卡，则自动从基本信息补建默认卡")
    @GetMapping("/card/list/{accountId}")
    public CommonResult<List<UserMedicalCardDTO>> listMedicalCard(@PathVariable Long accountId) {
        if (!powerAccountService.count(accountId)) {
            return CommonResult.validateFailed("不存在，该账号编号！");
        }

        List<UserMedicalCardDTO> cards = medicalCardService.list(accountId);

        // 老账号兼容：若无就诊卡，自动补建一张默认卡
        if (cards.isEmpty()) {
            // 通过 PowerAccount 获取手机号（account.name 即为注册手机号）
            PowerAccount account = powerAccountMapper.selectByPrimaryKey(accountId);
            if (account != null) {
                String phone = account.getName();
                // 尝试从 UserBasicInfo 获取用户姓名
                String name = "待完善";
                Optional<UserBasicInfo> infoOpt = userBasicInfoService.getOptionalByPhone(phone);
                if (infoOpt.isPresent() && infoOpt.get().getName() != null) {
                    name = infoOpt.get().getName();
                }
                LOGGER.info("老账号 accountId={} 无就诊卡，自动补建默认卡: name={}, phone={}", accountId, name, phone);
                if (medicalCardService.initDefaultCard(accountId, name, phone)) {
                    cards = medicalCardService.list(accountId);
                }
            }
        }

        return CommonResult.success(cards);
    }

    @Operation(summary = "删除就诊卡", description = "传入 关系编号")
    @DeleteMapping("/card/{relationId}")
    public CommonResult<String> deleteMedicalCard(@PathVariable Long relationId) {
        if (!medicalCardService.countRelation(relationId)) {
            return CommonResult.validateFailed("不存在，该关系编号！");
        }

        if (medicalCardService.delete(relationId)) {
            return CommonResult.success();
        }

        return CommonResult.failed("服务器错误，请联系管理员！");
    }


    @Operation(summary = "检查就诊卡数目是否超过限制", description = "传入 账号编号")
    @GetMapping("/card/number/{accountId}")
    public CommonResult<Boolean> countMedicalCard(@PathVariable Long accountId) {
        if (!powerAccountService.count(accountId)) {
            return CommonResult.validateFailed("不存在，该账号编号！");
        }

        return CommonResult.success(medicalCardService.count(accountId) > MAX_CARD_NUMBER);
    }

    @Operation(summary = "检查就诊卡信息是否存在", description = "传入 身份证编号")
    @GetMapping("/card/identification/{identificationNumber}")
    public CommonResult<Boolean> countIdentificationNumber(@PathVariable String identificationNumber) {
        return CommonResult.success(medicalCardService.countIdentificationNumber(identificationNumber));
    }

}
