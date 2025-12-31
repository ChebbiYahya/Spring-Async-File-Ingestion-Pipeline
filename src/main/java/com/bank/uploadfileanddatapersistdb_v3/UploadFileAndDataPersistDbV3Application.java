package com.bank.uploadfileanddatapersistdb_v3;

import com.bank.uploadfileanddatapersistdb_v3.config.DataFoldersProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
//@EnableConfigurationProperties(DataFoldersProperties.class)
public class UploadFileAndDataPersistDbV3Application {

	public static void main(String[] args) {
		SpringApplication.run(UploadFileAndDataPersistDbV3Application.class, args);
	}

}
