<<<<<<<< HEAD:services/news-service/src/main/java/com/newnormallist/newsservice/NewsServiceApplication.java
package com.newnormallist.newsservice;
========
package com.newsservice;
>>>>>>>> bf30fb11129aa33febd645fa4c401aec639bf7df:services/news-service/src/main/java/com/newsservice/NewsServiceApplication.java

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
