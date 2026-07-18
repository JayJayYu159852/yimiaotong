package cn.liyu.hospital.config;

import cn.liyu.hospital.common.security.IDynamicSecurityService;
import cn.liyu.hospital.entity.PowerResource;
import cn.liyu.hospital.service.IPowerAccountService;
import cn.liyu.hospital.service.IPowerResourceService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.userdetails.UserDetailsService;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 医院安全配置 — 提供 UserDetailsService 和动态权限数据源
 *
 * @author 医秒通
 */

@Configuration
public class HospitalSecurityConfig {

    @Resource
    private IPowerAccountService accountService;

    @Resource
    private IPowerResourceService resourceService;

    @Bean
    public UserDetailsService userDetailsService() {
        // 获取登录用户信息
        return username -> accountService.loadUserByUserName(username);
    }

    @Bean
    public IDynamicSecurityService dynamicSecurityService() {
        return () -> {
            Map<String, ConfigAttribute> map = new ConcurrentHashMap<>();

            List<PowerResource> resourceList = resourceService.listAll();

            for (PowerResource resource : resourceList) {
                map.put(resource.getUrl(), new org.springframework.security.access.SecurityConfig(resource.getId() + ":" + resource.getName()));
            }

            return map;
        };
    }
}
