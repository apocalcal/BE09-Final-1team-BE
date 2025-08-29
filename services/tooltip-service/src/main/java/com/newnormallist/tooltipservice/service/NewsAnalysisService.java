package com.newnormallist.tooltipservice.service;

import com.newnormallist.tooltipservice.dto.ProcessContentRequest;
import com.newnormallist.tooltipservice.dto.ProcessContentResponse;
import com.newnormallist.tooltipservice.dto.TermDetailResponseDto;
import com.newnormallist.tooltipservice.dto.TermDefinitionResponseDto;
import com.newnormallist.tooltipservice.entity.VocabularyTerm;
import com.newnormallist.tooltipservice.repository.VocabularyTermRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsAnalysisService {

    private final VocabularyTermRepository vocabularyTermRepository;
    private final NlpService nlpService;

    // Redis 캐시로 변경됨 - 메모리 캐시 제거

    /**
     * 뉴스 본문을 분석하여 어려운 단어에 마크업을 추가합니다.
     */
    public ProcessContentResponse processContent(ProcessContentRequest request) {
        log.info("뉴스 ID {}의 본문 분석을 시작합니다.", request.newsId());

        // Redis 캐시에서 어려운 단어 목록을 가져와서 마크업 처리
        Set<String> difficultWords = getDifficultWordsFromCache();
        String analyzedContent = getAnalyzedContent(request.newsId(), request.originalContent(), difficultWords);

        return new ProcessContentResponse(analyzedContent);
    }

    /**
     * Redis 캐시에서 어려운 단어 목록을 조회합니다.
     * 캐시 미스 시 DB에서 로드하여 캐시에 저장합니다.
     */
    @Cacheable(value = "difficultWords", key = "'all'")
    public Set<String> getDifficultWordsFromCache() {
        log.info("Redis 캐시 미스! DB에서 어려운 단어 목록을 로드합니다...");
        List<VocabularyTerm> allTerms = vocabularyTermRepository.findAll();
        
        Set<String> difficultWords = allTerms.stream()
                .map(VocabularyTerm::getTerm)
                .peek(term -> log.debug("DB에서 로드된 어려운 단어: '{}'", term))
                .collect(java.util.stream.Collectors.toSet());
        
        log.info("총 {}개의 어려운 단어를 Redis 캐시에 저장합니다.", difficultWords.size());
        
        if (difficultWords.isEmpty()) {
            log.warn("⚠️ DB에 vocabulary_term 데이터가 없습니다!");
        }
        
        return difficultWords;
    }

    @Cacheable(value = "processedContent", key = "#newsId")
    public String getAnalyzedContent(Long newsId, String originalContent, Set<String> difficultWords) {
        log.info("캐시 미스 발생! 뉴스 ID {}에 대한 NLP 분석을 시작합니다.", newsId);
        return nlpService.markupDifficultWords(originalContent, difficultWords);
    }

    // --- 반환 타입 수정됨 ---
    @Transactional(readOnly = true)
    @Cacheable(value = "termDetails", key = "#term.toLowerCase()")
    public TermDetailResponseDto getTermDefinitions(String term) {
        log.info("DB에서 '{}' 단어의 정의를 조회합니다.", term);
        
        // 먼저 정확 일치로 찾아보고, 없으면 부분 일치로 찾기
        VocabularyTerm vocabularyTerm = vocabularyTermRepository.findByTerm(term)
                .or(() -> vocabularyTermRepository.findByTermStartingWith(term))
                .orElseThrow(() -> new NoSuchElementException("단어를 찾을 수 없습니다: " + term));

        // 정의 목록을 displayOrder 순서대로 정렬하여 DTO로 변환
        List<TermDefinitionResponseDto> definitionDtos = vocabularyTerm.getDefinitions().stream()
                .sorted((def1, def2) -> {
                    // displayOrder가 null인 경우를 대비해 안전하게 정렬
                    Integer order1 = def1.getDisplayOrder() != null ? def1.getDisplayOrder() : Integer.MAX_VALUE;
                    Integer order2 = def2.getDisplayOrder() != null ? def2.getDisplayOrder() : Integer.MAX_VALUE;
                    return order1.compareTo(order2);
                })
                .map(def -> new TermDefinitionResponseDto(
                        def.getDefinition(),
                        def.getDisplayOrder()
                ))
                .collect(Collectors.toList());

        // DB에 저장된 단어 그대로 반환 (한자 포함)
        return new TermDetailResponseDto(vocabularyTerm.getTerm(), definitionDtos);
    }

    /**
     * 어려운 단어 캐시를 강제로 갱신합니다. (관리자용)
     * DB에 새로운 어려운 단어가 추가되었을 때 호출
     */
    @CacheEvict(value = "difficultWords", key = "'all'")
    public void refreshDifficultWordsCache() {
        log.info("어려운 단어 캐시를 강제로 갱신합니다.");
        // 캐시 삭제만 하고, 다음 요청에서 자동으로 새로 로드됨
    }
}