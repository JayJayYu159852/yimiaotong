package cn.liyu.hospital;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author 医秒通
 */

@SpringBootApplication
@EnableScheduling
public class HospitalApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(HospitalApplication.class, args);
    }

}
