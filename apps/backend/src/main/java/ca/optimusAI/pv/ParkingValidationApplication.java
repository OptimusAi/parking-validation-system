package ca.optimusAI.pv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
class ParkingValidationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ParkingValidationApplication.class, args);
    }
}
