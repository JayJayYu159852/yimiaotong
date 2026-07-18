package cn.liyu.hospital.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * Mybatis 配置类
 *
 * @author 医秒通
 */

@MapperScan("cn.liyu.hospital.mapper")
@Configuration
public class MybatisConfig {
}
