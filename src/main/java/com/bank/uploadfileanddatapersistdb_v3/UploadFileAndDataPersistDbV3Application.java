package com.bank.uploadfileanddatapersistdb_v3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class UploadFileAndDataPersistDbV3Application {

	public static void main(String[] args) {
		SpringApplication.run(UploadFileAndDataPersistDbV3Application.class, args);
	}

}
