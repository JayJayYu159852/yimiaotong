package cn.liyu.hospital.service.impl;

import cn.liyu.hospital.dto.WebLogDTO;
import cn.liyu.hospital.entity.LogOperation;
import cn.liyu.hospital.entity.LogOperationExample;
import cn.liyu.hospital.mapper.LogOperationMapper;
import cn.liyu.hospital.service.ILogOperationService;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import cn.hutool.core.util.StrUtil;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @author 医秒通
 */

@Service
public class LogOperationServiceImpl implements ILogOperationService {

    @Resource
    private LogOperationMapper operationMapper;

    /**
     * 创建操作记录
     *
     * @param dto 记录参数
     * @return 是否成功
     */
    @Override
    public boolean insert(WebLogDTO dto) {
        LogOperation record = new LogOperation();

        BeanUtils.copyProperties(dto, record);

        record.setResult(dto.getResult().toString());

        if (dto.getParameter() != null) {
            record.setParameter(dto.getParameter().toString());
        }

        Date date = new Date();

        record.setGmtCreate(date);
        record.setGmtModified(date);

        return operationMapper.insertSelective(record) > 0;
    }

    /**
     * 查找操作记录
     *
     * @param accountName 用户名称
     * @param method      请求方法
     * @param pageNum     第几页
     * @param pageSize    页大小
     * @return 操作记录表
     */
    @Override
    public List<LogOperation> search(String accountName, String method, Integer pageNum, Integer pageSize) {

        PageHelper.startPage(pageNum, pageSize);

        LogOperationExample example = new LogOperationExample();

        LogOperationExample.Criteria criteria = example.createCriteria();

        if (StrUtil.isNotEmpty(accountName)) {
            criteria.andAccountNameEqualTo(accountName);
        }

        if (StrUtil.isNotEmpty(method)) {
            criteria.andMethodEqualTo(method);
        }

        return operationMapper.selectByExampleWithBLOBs(example);
    }
}
