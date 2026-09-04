package com.jobsight.company;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(info = @Info(
		title = "JobSight Company API",
		version = "0.0.6",
		description = """
				계정별로 분리된 기업 정보 CRUD.
				인증은 Google OIDC 세션 쿠키. 변경 요청은 XSRF-TOKEN 쿠키 값을 X-XSRF-TOKEN 헤더로 보내야 한다.
				이 문서는 ADMIN 세션에서만 열린다."""
))
@SpringBootApplication
public class CompanyApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(CompanyApiApplication.class, args);
	}

}
