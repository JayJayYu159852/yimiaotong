package cn.liyu.hospital.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 钱包余额 DTO
 *
 * @author 医秒通
 */
@Data
public class WalletDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long walletId;
    private Long accountId;
    private Long balance;
    private String balanceYuan;
}
