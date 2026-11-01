package proov.global.common.entity

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.SQLRestriction
import java.time.Instant

/**
 * 삭제 대상 엔티티는 별도로 @SQLDelete를 선언해 물리 삭제를 막는다.
 */
@MappedSuperclass
@SQLRestriction("deleted_at is null")
abstract class BaseEntity : BaseCreatedEntity() {

    @Column(insertable = false, updatable = false, comment = "삭제 일시")
    var deletedAt: Instant? = null
        protected set
}
