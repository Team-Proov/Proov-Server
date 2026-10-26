package proov.global.id

import org.hibernate.annotations.IdGeneratorType

@IdGeneratorType(TsidGenerator::class)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class TsidId
