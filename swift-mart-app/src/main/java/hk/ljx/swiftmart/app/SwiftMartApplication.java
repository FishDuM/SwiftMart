package hk.ljx.swiftmart.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"hk.ljx.swiftmart.*"})
public class SwiftMartApplication {
    public static void main( String[] args ){
        SpringApplication.run(SwiftMartApplication.class, args);
    }
}
