package kopo.poly;


/**
 * 체크리스트 기준 주석: 개발환경 세팅: Spring Boot 애플리케이션의 시작점을 담당한다.
 */
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@MapperScan("kopo.poly.mapper")
@SpringBootApplication
public class Deepfake2Application extends SpringBootServletInitializer {

    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        SpringApplication.run(Deepfake2Application.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Deepfake2Application.class);
    }
}
