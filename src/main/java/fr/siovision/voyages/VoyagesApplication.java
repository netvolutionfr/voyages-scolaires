package fr.siovision.voyages;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan  // <— scanne les @ConfigurationProperties
public class VoyagesApplication {

    public static void main(String[] args) {
        SpringApplication.run(VoyagesApplication.class, args);
    }

}
