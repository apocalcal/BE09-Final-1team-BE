<<<<<<<< HEAD:.history/services/news-service/src/main/java/com/newsservice/news/NewsServiceApplication_20250810133328.java
package com.newsservice.news;
========
package com.newsservice;
>>>>>>>> develop:services/news-service/src/main/java/com/newsservice/NewsServiceApplication.java

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class NewsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NewsServiceApplication.class, args);
    }
}
