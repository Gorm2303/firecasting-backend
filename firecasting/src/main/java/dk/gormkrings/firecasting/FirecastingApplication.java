package dk.gormkrings.firecasting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication(
		excludeName = {
				"org.springframework.modulith.events.jpa.JpaEventPublicationAutoConfiguration"
		},
		exclude = {
				DataSourceAutoConfiguration.class  // exclude if you don't need a database
		}
)public class FirecastingApplication {

	public static void main(String[] args) {
		SpringApplication.run(FirecastingApplication.class, args);
	}

}

@RestController
class HelloController {

	@GetMapping("/")
	public String hello() {
		return "Hello, World!";
	}
}