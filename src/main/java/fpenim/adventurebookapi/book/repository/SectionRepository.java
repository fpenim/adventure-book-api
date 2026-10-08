package fpenim.adventurebookapi.book.repository;

import fpenim.adventurebookapi.book.model.section.SectionType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SectionRepository extends JpaRepository<SectionEntity, SectionEntity.SectionId> {

    @Query("select s.id.number from SectionEntity s where s.id.bookId = :bookId and s.type = :type")
    Optional<Integer> findSectionIdByType(@Param("bookId") Long bookId, @Param("type") SectionType type);

    @Query("""
        select s.id.bookId as bookId, s.id.number as sectionId
            from SectionEntity s
            where s.type = 'BEGIN'
        """)
    List<BeginSection> findBeginSections();
}
