package spring.ai.philoagents;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(PhiloTestConfiguration.class)
public class TestApplication {

}
