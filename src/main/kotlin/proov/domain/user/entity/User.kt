package proov.domain.user.entity

import io.hypersistence.utils.hibernate.id.Tsid
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import proov.domain.user.enums.SocialProvider
import proov.global.common.entity.BaseCreatedEntity

@Entity
@Table(
    name = "users",
    uniqueConstraints = [UniqueConstraint(name = "uk_users_social_account", columnNames = ["social_provider", "social_id"])],
)
class User protected constructor(
    socialProvider: SocialProvider,
    socialId: String,
) : BaseCreatedEntity() {

    @Id
    @Tsid
    @Column(length = 13, comment = "회원 ID")
    lateinit var id: String
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, comment = "소셜 로그인 제공자")
    var socialProvider: SocialProvider = socialProvider
        protected set

    @Column(nullable = false, comment = "소셜 계정 식별자")
    var socialId: String = socialId
        protected set
}
