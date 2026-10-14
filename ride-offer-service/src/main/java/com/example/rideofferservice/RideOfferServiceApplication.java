package com.example.rideofferservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;

@SpringBootApplication
@EnableRabbit
public class RideOfferServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(RideOfferServiceApplication.class, args);
	}
}
