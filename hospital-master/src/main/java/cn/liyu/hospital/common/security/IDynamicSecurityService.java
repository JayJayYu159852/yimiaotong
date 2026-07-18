package cn.liyu.hospital.common.security;

import org.springframework.security.access.ConfigAttribute;

import java.util.Map;

/**
 * @author 医秒通
 */

public interface IDynamicSecurityService {
    /**
     * 加载 资源 ANT 通配符和 资源对应MAP
     *
     * @return 资源 ANT 通配符和 资源
     */
    Map<String, ConfigAttribute> loadDataSource();
}
