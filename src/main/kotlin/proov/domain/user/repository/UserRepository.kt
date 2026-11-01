package proov.domain.user.repository

import org.springframework.data.jpa.repository.JpaRepository
import proov.domain.user.entity.User
import proov.domain.user.enums.SocialProvider

interface UserRepository : JpaRepository<User, String> {
    fun findBySocialProviderAndSocialId(socialProvider: SocialProvider, socialId: String): User?
}
