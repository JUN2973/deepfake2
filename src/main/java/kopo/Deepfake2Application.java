package kopo.poly;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Spring Boot 애플리케이션을 시작하는 메인 진입점이다.
 */
@MapperScan("kopo.poly.mapper")
@SpringBootApplication
public class Deepfake2Application extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(Deepfake2Application.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Deepfake2Application.class);
    }
}
