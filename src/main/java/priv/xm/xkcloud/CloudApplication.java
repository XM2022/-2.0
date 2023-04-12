package priv.xm.xkcloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

@EnableAutoConfiguration(exclude = {MultipartAutoConfiguration.class})
@SpringBootApplication
@PropertySource({"classpath:ThreadPoolConfiguration.properties"})
@EnableTransactionManagement  
public class CloudApplication{

    public static void main(String[] args) {
        SpringApplication.run(CloudApplication.class, args);
    }

    @Bean(name = "multipartResolver")
    public MultipartResolver multipartResolver() {
        CommonsMultipartResolver resolver = new CommonsMultipartResolver();
        resolver.setDefaultEncoding("UTF-8");
        resolver.setResolveLazily(false); //前端进行了优化,关闭提高上传性能.  true:推迟文件解析，以便检测文件大小超出异常.
        resolver.setMaxInMemorySize(10*1024*1024);
        resolver.setMaxUploadSize(50*1024*1024); //上传文件大小50MB
        resolver.setMaxUploadSizePerFile(50*1024*1024); //上传文件总大小50MB
        return resolver;
    }
}

