package io.enigmarobotics.discordbroadcastservice;

import io.enigmarobotics.discordbroadcastservice.db.repositories.CustomerRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories(basePackageClasses = CustomerRepository.class)
public class DiscordBroadcastServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscordBroadcastServiceApplication.class, args);
    }

}