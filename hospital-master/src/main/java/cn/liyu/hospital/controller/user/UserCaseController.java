package cn.liyu.hospital.controller.user;

import cn.liyu.hospital.common.api.CommonResult;
import cn.liyu.hospital.common.security.UserHolder;
import cn.liyu.hospital.dto.UserCaseDTO;
import cn.liyu.hospital.dto.UserMedicalCardDTO;
import cn.liyu.hospital.entity.UserCase;
import cn.liyu.hospital.entity.VisitAppointment;
import cn.liyu.hospital.service.IHospitalDoctorService;
import cn.liyu.hospital.service.IUserCaseService;
import cn.liyu.hospital.service.IUserMedicalCardService;
import cn.liyu.hospital.service.IVisitAppointmentService;
import cn.liyu.hospital.service.IVisitPlanService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 用户端 · 病例管理 —— 仅限查看自己的病例
 * <p>
 * 添加/更新/删除/列表 等操作已迁移至管理端 UserCaseController
 *
 * @author 医秒通
 */

@Tag(name = "用户端 · 病例管理", description = "用户查看病例接口")
@RestController
@RequestMapping("/user")
public class UserCaseController {

    @Resource
    private IUserCaseService caseService;

    @Resource
    private IUserMedicalCardService cardService;

    @Resource
    private IVisitAppointmentService appointmentService;

    @Resource
    private IVisitPlanService visitPlanService;

    @Resource
    private IHospitalDoctorService hospitalDoctorService;

    @Operation(summary = "查看就诊病例详情", description = "传入 就诊卡编号、预约编号")
    @GetMapping("/case")
    public CommonResult<UserCaseDTO> getUserCase(@RequestParam Long cardId,
                                                 @RequestParam Long appointmentId,
                                                 Principal principal) {

        if (!cardService.countCardId(cardId)) {
            return CommonResult.validateFailed("不存在，该就诊卡编号！");
        }

        return CommonResult.success(convert(caseService.get(cardId, appointmentId)));
    }

    @Operation(summary = "查看我的全部病例", description = "无需参数，自动查当前用户所有就诊卡的病例")
    @GetMapping("/case/mine")
    public CommonResult<List<UserCaseDTO>> getMyCases(Principal principal) {
        Long userId = UserHolder.getUser().getId();
        List<UserMedicalCardDTO> cards = cardService.list(userId);
        if (cards == null || cards.isEmpty()) {
            return CommonResult.success(new ArrayList<>());
        }
        List<UserCaseDTO> allCases = new ArrayList<>();
        for (UserMedicalCardDTO card : cards) {
            List<UserCase> cardCases = caseService.list(card.getId(), 1, 200);
            if (cardCases != null) {
                cardCases.forEach(userCase -> allCases.add(convert(userCase)));
            }
        }
        return CommonResult.success(allCases);
    }

    /**
     * 转换为病例封装对象：聚合医生/医院/专科中文名称
     * <p>
     * 优先从预约关联的出诊计划取（一次取全三个名称，详情查询均走缓存）；
     * 预约信息缺失时回退到医生信息兜底。
     *
     * @param userCase 病例记录
     * @return 病例封装对象
     */
    private UserCaseDTO convert(UserCase userCase) {

        if (userCase == null) {
            return null;
        }

        UserCaseDTO dto = new UserCaseDTO();

        BeanUtils.copyProperties(userCase, dto);

        // 通过预约 → 出诊计划，聚合医生/医院/专科名称
        Optional<VisitAppointment> appointment = appointmentService.getOptional(userCase.getAppointmentId());

        if (appointment.isPresent()) {
            visitPlanService.getOptional(appointment.get().getPlanId()).ifPresent(plan -> {
                dto.setDoctorName(plan.getDoctorName());
                dto.setHospitalName(plan.getHospitalName());
                dto.setSpecialName(plan.getSpecialName());
            });
        }

        // 兜底：预约/计划信息缺失时，至少给出医生名称
        if (dto.getDoctorName() == null && userCase.getDoctorId() != null) {
            dto.setDoctorName(hospitalDoctorService.getName(userCase.getDoctorId()));
        }

        return dto;
    }
}
