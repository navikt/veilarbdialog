package no.nav.fo.veilarbdialog;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class Application {

    public static void main(String... args) {
        SpringApplication.run(Application.class, args);
    }

}
