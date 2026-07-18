package cn.liyu.hospital.mapper;

import cn.liyu.hospital.entity.PaymentWallet;
import org.apache.ibatis.annotations.Param;

/**
 * 钱包 Mapper
 * @author 医秒通
 */
public interface PaymentWalletMapper {
    int deleteByPrimaryKey(Long id);

    int insert(PaymentWallet record);

    int insertSelective(PaymentWallet record);

    PaymentWallet selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(PaymentWallet record);

    int updateByPrimaryKey(PaymentWallet record);

    /**
     * 根据账号编号查询钱包
     */
    PaymentWallet selectByAccountId(@Param("accountId") Long accountId);

    /**
     * 乐观锁扣减余额
     * @return 影响行数（0表示乐观锁冲突或余额不足）
     */
    int deductBalance(@Param("walletId") Long walletId,
                      @Param("amount") Long amount,
                      @Param("version") Integer version);

    /**
     * 增加余额（退款时使用）
     */
    int addBalance(@Param("walletId") Long walletId,
                   @Param("amount") Long amount);
}
