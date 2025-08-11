-- 뉴스레터 구독자 테이블 생성
CREATE TABLE IF NOT EXISTS newsletter_subscribers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    token VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP NULL,
    source_ip VARCHAR(45) NULL,
    INDEX idx_email (email),
    INDEX idx_token (token),
    INDEX idx_confirmed (confirmed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 카테고리 enum 사용을 위한 테이블 수정
-- news_crawl 테이블의 category_id 컬럼을 category enum 컬럼으로 변경
ALTER TABLE news_crawl 
DROP FOREIGN KEY IF EXISTS fk_news_crawl_category,
DROP COLUMN IF EXISTS category_id,
ADD COLUMN category VARCHAR(20) NOT NULL DEFAULT 'SOCIETY' 
COMMENT '카테고리 enum: POLITICS, ECONOMY, SOCIETY, CULTURE, INTERNATIONAL, IT_SCIENCE';

-- news 테이블의 category_id 컬럼을 category enum 컬럼으로 변경
ALTER TABLE news 
DROP FOREIGN KEY IF EXISTS fk_news_category,
DROP COLUMN IF EXISTS category_id,
ADD COLUMN category VARCHAR(20) NOT NULL DEFAULT 'SOCIETY' 
COMMENT '카테고리 enum: POLITICS, ECONOMY, SOCIETY, CULTURE, INTERNATIONAL, IT_SCIENCE';

-- category 테이블 삭제 (enum 사용으로 불필요)
DROP TABLE IF EXISTS category;
