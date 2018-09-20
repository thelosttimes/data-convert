package com.my.dataconvert.start;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


/**
 * @author 997620782@qq.com
 */
@Configuration  
@ComponentScan(basePackages = "com.my.dataconvert")
//@Import(DynamicDataSourceRegister.class)
@SpringBootApplication
public class Main extends SpringBootServletInitializer{
	
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		builder.sources(this.getClass());
		return super.configure(builder);
	}
	
	public static void main(String[] args) {
		SpringApplication.run(Main.class);
	}
}