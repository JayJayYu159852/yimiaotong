package cn.liyu.hospital.service;

/**
 * @author 医秒通
 */

public interface IVisitBlacklistService {

    /**
     * 插入黑名单
     *
     * @param cardId 就诊卡号
     * @return 是否成功
     */
    boolean insert(Long cardId);

    /**
     * 判断是否被禁用
     *
     * @param cardId 就诊卡号
     * @return 是否禁用
     */
    boolean isForbid(Long cardId);

    /**
     * 自动解封，到期的用户
     */
    void autoUnlock();
}
