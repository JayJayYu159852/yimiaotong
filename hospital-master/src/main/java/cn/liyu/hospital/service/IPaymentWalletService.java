package cn.liyu.hospital.service;

import cn.liyu.hospital.entity.PaymentWallet;

import java.util.Optional;

/**
 * 钱包服务接口
 *
 * @author 医秒通
 */
public interface IPaymentWalletService {

    /** 初始化钱包（新用户注册时调用，默认1000分=10元） */
    boolean initWallet(Long accountId);

    /** 根据账号ID查询钱包 */
    Optional<PaymentWallet> getByAccountId(Long accountId);

    /** 查询余额（分） */
    Long getBalance(Long accountId);

    /** 乐观锁扣减余额 */
    boolean deduct(Long walletId, Long amount, Integer version);

    /** 退款时增加余额 */
    boolean addBalance(Long walletId, Long amount);

    /** 判断钱包是否存在 */
    boolean exist(Long accountId);
}
