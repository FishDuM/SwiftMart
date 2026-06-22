package hk.ljx.swiftmart.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"hk.ljx.swiftmart.*"})
@MapperScan("hk.ljx.swiftmart.common.domain.mapper")
public class SwiftMartApplication {
    public static void main( String[] args ){
        SpringApplication.run(SwiftMartApplication.class, args);
    }
}
