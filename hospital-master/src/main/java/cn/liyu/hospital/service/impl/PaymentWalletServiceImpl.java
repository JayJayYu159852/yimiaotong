package cn.liyu.hospital.service.impl;

import cn.liyu.hospital.entity.PaymentWallet;
import cn.liyu.hospital.mapper.PaymentWalletMapper;
import cn.liyu.hospital.service.IPaymentWalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Optional;

/**
 * 钱包服务实现
 *
 * @author 医秒通
 */
@Service
public class PaymentWalletServiceImpl implements IPaymentWalletService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentWalletServiceImpl.class);

    /** 新用户默认余额（分），默认1000分=10元 */
    @Value("${payment.default-balance:1000}")
    private Long defaultBalance;

    @Resource
    private PaymentWalletMapper walletMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean initWallet(Long accountId) {
        // 幂等：已存在则跳过
        if (exist(accountId)) {
            LOGGER.info("钱包已存在，跳过初始化: accountId={}", accountId);
            return true;
        }

        PaymentWallet wallet = new PaymentWallet();
        wallet.setAccountId(accountId);
        wallet.setBalance(defaultBalance);
        wallet.setVersion(0);
        wallet.setGmtCreate(new Date());
        wallet.setGmtModified(new Date());

        int rows = walletMapper.insertSelective(wallet);
        LOGGER.info("钱包初始化成功: accountId={}, balance={}分", accountId, defaultBalance);
        return rows > 0;
    }

    @Override
    public Optional<PaymentWallet> getByAccountId(Long accountId) {
        return Optional.ofNullable(walletMapper.selectByAccountId(accountId));
    }

    @Override
    public Long getBalance(Long accountId) {
        PaymentWallet wallet = walletMapper.selectByAccountId(accountId);
        return wallet != null ? wallet.getBalance() : 0L;
    }

    @Override
    public boolean deduct(Long walletId, Long amount, Integer version) {
        int rows = walletMapper.deductBalance(walletId, amount, version);
        return rows > 0;
    }

    @Override
    public boolean addBalance(Long walletId, Long amount) {
        int rows = walletMapper.addBalance(walletId, amount);
        return rows > 0;
    }

    @Override
    public boolean exist(Long accountId) {
        return walletMapper.selectByAccountId(accountId) != null;
    }
}
