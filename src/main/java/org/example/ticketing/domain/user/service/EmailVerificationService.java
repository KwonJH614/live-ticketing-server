package org.example.ticketing.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ticketing.common.exception.EmailSendException;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {
  private final JavaMailSender mailSender;
  private final ReactiveRedisTemplate<String, String> redisTemplate;

  private static final String VERIFICATION_CODE_PREFIX = "verification_code_";
  private static final int CODE_LENGTH = 6;
  private static final int EXPIRATION_MINUTES = 10;

  public Mono<Void> sendVerificationCode(String email) {
    String code = generateVerificationCode();
    String key = VERIFICATION_CODE_PREFIX + email;

    return redisTemplate.opsForValue()
            .set(key, code, Duration.ofMinutes(EXPIRATION_MINUTES))
            .flatMap(result -> {
              if (Boolean.TRUE.equals(result)) {
                return sendVerificationEmail(email, code);
              }
              return Mono.error(new EmailSendException("인증 코드 저장에 실패했습니다"));
            })
            .doOnSuccess(v -> log.info("인증 코드가 {}로 전송되었습니다", email));
  }

  public Mono<Boolean> verifyCode(String email, String code) {
    String key = VERIFICATION_CODE_PREFIX + email;

    return redisTemplate.opsForValue()
            .get(key)
            .map(storedCode -> storedCode.equals(code))
            .defaultIfEmpty(false)
            .flatMap(isValid -> {
              if (isValid) {
                return redisTemplate.delete(key)
                        .thenReturn(true);
              }
              return Mono.just(false);
            })
            .doOnNext(isValid -> {
              if (isValid) {
                log.info("이메일 {} 인증이 완료되었습니다.", email);
              } else {
                log.warn("이메일 {} 인증 실패 - 잘못된 코드: {}", email, code);
              }
            });

  }

  private Mono<Void> sendVerificationEmail(String email, String code) {
    return Mono.<Void>fromRunnable(() -> {
      try {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("티켓팅 서비스 이메일 인증");
        message.setText(String.format(
                "안녕하세요!\n\n" +
                        "티켓팅 서비스 회원가입을 위한 인증 코드입니다.\n\n" +
                        "인증 코드: %s\n\n" +
                        "이 코드는 %d분간 유효합니다.\n" +
                        "본인이 요청하지 않았다면 이 이메일을 무시해주세요.\n\n" +
                        "감사합니다.",
                code, EXPIRATION_MINUTES
        ));

        mailSender.send(message);
      } catch (Exception e) {
        log.error("이메일 전송 실패: {} - {}", email, e.getMessage());
        throw new EmailSendException("이메일 전송에 실패했습니다.");
      }
    }).subscribeOn(Schedulers.boundedElastic());
  }

  private String generateVerificationCode() {
    Random random = new Random();
    StringBuilder builder = new StringBuilder();

    for (int i = 0; i < CODE_LENGTH; i++) {
      builder.append(random.nextInt(10));
    }

    return builder.toString();
  }
}
