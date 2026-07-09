package com.mmetzner.vmh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class VmhApplication {
	public static void main(String[] args) {
		SpringApplication.run(VmhApplication.class, args);
	}
}
