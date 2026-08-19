package kopo.poly.document;


/**
 * 체크리스트 기준 주석: 컬렉션 정의서(NoSQL): MongoDB에 저장되는 뉴스 캐시 문서 구조를 정의한다.
 */
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * MongoDB에 저장하는 뉴스 검색 캐시 문서 모델이다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "news_cache")
public class NewsCacheDocument {

    @Id
    private String id;

    private String query;
    private String title;
    private String originallink;
    private String link;
    private String description;
    private String provider;
    private String pubDate;
    @Transient
    private String articleContent;

    private LocalDateTime createdAt;

    @Indexed(name = "ttl_expire_at", expireAfterSeconds = 0)
    private LocalDateTime expireAt;
}
