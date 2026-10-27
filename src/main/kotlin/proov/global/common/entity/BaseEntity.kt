package proov.global.common.entity

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.SQLRestriction
import java.time.Instant

@MappedSuperclass
@SQLRestriction("deleted_at is null")
abstract class BaseEntity : BaseCreatedEntity() {

    @Column
    var deletedAt: Instant? = null
        protected set

    val isDeleted: Boolean
        get() = deletedAt != null

    fun softDelete(now: Instant) {
        if (deletedAt == null) deletedAt = now
    }
}
