package ziang.com.it4409.noteapp.note;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    Optional<Note> findByIdAndUserId(Long id, Long userId);

    @Query("""
            select n from Note n
            where n.user.id = :userId
              and (:category is null or n.category = :category)
              and (
                    :keyword = ''
                    or lower(n.title) like lower(concat('%', :keyword, '%'))
                    or lower(n.content) like lower(concat('%', :keyword, '%'))
              )
            """)
    Page<Note> searchOwnedNotes(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("category") NoteCategory category,
            Pageable pageable
    );
}
