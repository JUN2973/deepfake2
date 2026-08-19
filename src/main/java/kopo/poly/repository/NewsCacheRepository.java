package kopo.poly.repository;


/**
 * 체크리스트 기준 주석: 컬렉션 정의서(NoSQL): MongoDB 뉴스 캐시 컬렉션 접근 Repository를 정의한다.
 */
import kopo.poly.document.NewsCacheDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * 뉴스 캐시 문서를 MongoDB에서 조회하고 삭제하는 Repository다.
 */
public interface NewsCacheRepository extends MongoRepository<NewsCacheDocument, String> {

    List<NewsCacheDocument> findByQueryOrderByCreatedAtDesc(String query);

    Optional<NewsCacheDocument> findById(String id);

    void deleteByQuery(String query);
}