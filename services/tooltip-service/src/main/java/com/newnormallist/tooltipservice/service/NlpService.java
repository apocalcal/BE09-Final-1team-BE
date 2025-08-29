package com.newnormallist.tooltipservice.service;

import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.Token;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class NlpService {

    private final Komoran komoran;

    public NlpService() {
        // Komoran 객체를 생성합니다. 모델은 경량화된 LIGHT 모델을 사용합니다.
        // 더 높은 정확도가 필요하면 DEFAULT_MODEL.FULL을 사용할 수 있습니다.
        this.komoran = new Komoran(DEFAULT_MODEL.LIGHT);
        log.info("Komoran 형태소 분석기 초기화 완료.");
    }

    /**
     * 원본 텍스트에서 어려운 단어를 찾아 span 태그로 감싸는 메소드
     * @param originalContent 원본 뉴스 기사 본문
     * @param difficultWords DB에서 가져온 어려운 단어 목록
     * @return span 태그가 삽입된 HTML 텍스트
     */
    public String markupDifficultWords(String originalContent, Set<String> difficultWords) {
        if (originalContent == null || originalContent.isBlank() || difficultWords == null || difficultWords.isEmpty()) {
            return originalContent;
        }

        long startTime = System.currentTimeMillis();

        // Komoran을 사용하여 텍스트를 형태소 단위로 분석합니다.
        List<Token> tokens = komoran.analyze(originalContent).getTokenList();
        
        // 디버깅: Komoran 분석 결과 로그 출력
        log.info("============= Komoran 형태소 분석 결과 =============");
        log.info("원본 텍스트: {}", originalContent);
        for (Token token : tokens) {
            log.info("형태소: '{}', 품사: '{}', 위치: {}-{}", 
                    token.getMorph(), token.getPos(), token.getBeginIndex(), token.getEndIndex());
        }
        log.info("=== 어려운 단어 캐시 내용 ===");
        log.info("캐시된 어려운 단어 개수: {}", difficultWords.size());
        difficultWords.forEach(word -> log.info("어려운 단어: '{}'", word));
        log.info("================================");
        
        StringBuilder markedUpContent = new StringBuilder();
        int lastIndex = 0;

        for (Token token : tokens) {
            int beginIndex = token.getBeginIndex();
            int endIndex = token.getEndIndex();
            
            // 토큰 위치 검증
            if (beginIndex < 0 || endIndex > originalContent.length() || beginIndex >= endIndex) {
                log.warn("⚠️ 잘못된 토큰 위치: '{}' ({}-{}), 스킵합니다.", token.getMorph(), beginIndex, endIndex);
                continue;
            }
            
            // 이전 토큰과 현재 토큰 사이의 공백이나 특수문자를 그대로 유지합니다.
            if (beginIndex > lastIndex) {
                markedUpContent.append(originalContent, lastIndex, beginIndex);
            } else if (beginIndex < lastIndex) {
                // 토큰이 겹치는 경우 - 이미 처리된 부분이므로 스킵
                log.debug("토큰 겹침 감지: '{}' ({}-{}), 이전 위치: {}", token.getMorph(), beginIndex, endIndex, lastIndex);
                continue;
            }

            String term = token.getMorph();
            String pos = token.getPos(); // 품사 (예: NNP-고유명사, NNG-일반명사)

            // 단어가 명사(NNG, NNP)이고, 어려운 단어 목록에 포함되어 있다면
            boolean isNoun = pos.equals("NNG") || pos.equals("NNP");
            boolean isDifficult = isMatchingDifficultWord(term, difficultWords);
            
            log.debug("형태소 '{}' 검사 - 명사여부: {}, 어려운단어여부: {}, 위치: {}-{}", 
                     term, isNoun, isDifficult, beginIndex, endIndex);
            
            if (isNoun && isDifficult) {
                // 툴팁 기능을 위한 span 태그를 추가합니다.
                log.info("✅ 마크업 적용: '{}'", term);
                String originalWord = originalContent.substring(beginIndex, endIndex);
                markedUpContent.append("<span class=\"tooltip-word\" data-term=\"").append(term).append("\">").append(originalWord).append("</span>");
            } else {
                // 그렇지 않으면 원본 텍스트를 그대로 추가합니다.
                String originalWord = originalContent.substring(beginIndex, endIndex);
                markedUpContent.append(originalWord);
            }
            lastIndex = endIndex;
        }

        // 마지막 토큰 이후의 나머지 텍스트를 추가합니다.
        if (lastIndex < originalContent.length()) {
            markedUpContent.append(originalContent.substring(lastIndex));
        }

        long endTime = System.currentTimeMillis();
        log.debug("NLP 마크업 처리 시간: {}ms", (endTime - startTime));

        return markedUpContent.toString();
    }

    /**
     * 추출된 단어가 어려운 단어 목록과 매칭되는지 확인하는 메서드
     * DB에 "예산 (豫算)", "예산 (Budget)" 형태로 저장되어 있어도 "예산"으로 매칭 가능
     */
    private boolean isMatchingDifficultWord(String extractedTerm, Set<String> difficultWords) {
        log.debug("매칭 검사 시작: '{}'", extractedTerm);
        
        // 1. 정확 일치 (가장 빠름)
        if (difficultWords.contains(extractedTerm)) {
            log.debug("정확 일치 발견: '{}'", extractedTerm);
            return true;
        }
        
        // 2. DB에 한자/영어 포함 형태로 저장된 경우 매칭
        // 예: extractedTerm="예산" → DB에서 "예산 (豫算)", "예산 (Budget)" 등을 찾음
        boolean found = difficultWords.stream()
                .anyMatch(dbTerm -> {
                    boolean match = isTermMatch(extractedTerm, dbTerm);
                    if (match) {
                        log.debug("패턴 매칭 성공: '{}' ↔ '{}'", extractedTerm, dbTerm);
                    }
                    return match;
                });
        
        if (!found) {
            log.debug("매칭되는 어려운 단어 없음: '{}'", extractedTerm);
        }
        
        return found;
    }

    /**
     * 추출된 단어와 DB 단어가 매칭되는지 판단
     * @param extractedTerm Komoran에서 추출된 순수 단어 (예: "예산")
     * @param dbTerm DB에 저장된 단어 (예: "예산 (豫算)", "예산 (Budget)")
     */
    private boolean isTermMatch(String extractedTerm, String dbTerm) {
        // DB 단어가 "단어 (한자/영어)" 패턴인지 확인
        if (dbTerm.contains(" (") && dbTerm.endsWith(")")) {
            // "예산 (豫算)" → "예산" 추출
            String baseWord = dbTerm.substring(0, dbTerm.indexOf(" (")).trim();
            return extractedTerm.equals(baseWord);
        }
        
        // 패턴이 아니면 정확 일치만 인정
        return extractedTerm.equals(dbTerm);
    }
}
