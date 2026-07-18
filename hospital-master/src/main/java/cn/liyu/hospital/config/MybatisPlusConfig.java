package cn.liyu.hospital.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类
 * <p>
 * 注意：分页功能仍由 PageHelper 接管（pagehelper-spring-boot-starter），
 * 此处不注册 PaginationInnerInterceptor，避免双分页插件冲突导致 SQL 报错。
 *
 * @author 医秒通
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * MyBatis-Plus 拦截器（不含分页插件，分页由 PageHelper 处理）
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 如需其他插件（如乐观锁、防全表更新删除、多租户等），在此添加
        // interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}
