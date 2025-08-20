package com.newsservice.news.repository;

import aj.org.objectweb.asm.commons.Remapper;
import org.springframework.stereotype.Repository;

@Repository
public class NewsSummaryRepository {
    public Remapper findByNewsIdAndResolvedTypeAndLinesAndPromptKey(long newsId, String t, int l, String p) {
    }
}
