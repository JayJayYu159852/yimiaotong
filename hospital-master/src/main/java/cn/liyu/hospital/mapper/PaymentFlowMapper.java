package cn.liyu.hospital.mapper;

import cn.liyu.hospital.entity.PaymentFlow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 支付流水 Mapper
 * @author 医秒通
 */
public interface PaymentFlowMapper {
    int deleteByPrimaryKey(Long id);

    int insert(PaymentFlow record);

    int insertSelective(PaymentFlow record);

    PaymentFlow selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(PaymentFlow record);

    int updateByPrimaryKey(PaymentFlow record);

    /**
     * 根据账号查询支付流水列表
     */
    List<PaymentFlow> selectByAccountId(@Param("accountId") Long accountId);
}
