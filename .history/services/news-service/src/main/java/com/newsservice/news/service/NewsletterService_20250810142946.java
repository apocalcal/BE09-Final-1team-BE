package com.newsservice.news.service;

import com.newsservice.news.dto.NewsletterSubscribeRequest;
import com.newsservice.news.entity.NewsletterSubscriber;
import com.newsservice.news.repository.NewsletterSubscriberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsletterService {

    private final NewsletterSubscriberRepository newsletterSubscriberRepository;
    private final JavaMailSender mailSender;

    /**
     * 뉴스레터 구독 처리
     */
    @Transactional
    public void subscribe(NewsletterSubscribeRequest request) {
        // 1. 이메일 중복 체크
        if (newsletterSubscriberRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 구독 중인 이메일입니다.");
        }

        // 2. 구독자 생성
        NewsletterSubscriber subscriber = NewsletterSubscriber.builder()
                .email(request.getEmail())
                .sourceIp(request.getSourceIp())
                .build();

        newsletterSubscriberRepository.save(subscriber);

        // 3. 확인 메일 발송
        sendConfirmationEmail(subscriber.getEmail(), subscriber.getToken());

        log.info("뉴스레터 구독 요청 완료 - 이메일: {}", request.getEmail());
    }

    /**
     * 구독 확인 처리
     */
    @Transactional
    public void confirmSubscription(String token) {
        NewsletterSubscriber subscriber = newsletterSubscriberRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 확인 토큰입니다."));

        if (subscriber.getConfirmed()) {
            throw new IllegalArgumentException("이미 확인된 구독입니다.");
        }

        subscriber.setConfirmed(true);
        subscriber.setConfirmedAt(LocalDateTime.now());
        newsletterSubscriberRepository.save(subscriber);

        log.info("뉴스레터 구독 확인 완료 - 이메일: {}", subscriber.getEmail());
    }

    /**
     * 확인된 구독자 수 조회
     */
    public long getConfirmedSubscriberCount() {
        return newsletterSubscriberRepository.countConfirmedSubscribers();
    }

    /**
     * 확인 메일 발송
     */
    private void sendConfirmationEmail(String email, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("뉴스레터 구독 확인");
        
        String confirmationUrl = "http://localhost:8082/api/newsletter/confirm?token=" + token;
        message.setText("뉴스레터 구독을 확인하려면 아래 링크를 클릭하세요:\n\n" + confirmationUrl + 
                       "\n\n이 링크는 24시간 동안 유효합니다.");
        
        mailSender.send(message);
        log.info("구독 확인 메일 발송 완료 - 이메일: {}", email);
    }
}
