package proov.global.id

import io.hypersistence.tsid.TSID
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.generator.BeforeExecutionGenerator
import org.hibernate.generator.EventType
import org.hibernate.generator.EventTypeSets
import java.util.EnumSet

class TsidGenerator : BeforeExecutionGenerator {

    override fun generate(
        session: SharedSessionContractImplementor,
        owner: Any,
        currentValue: Any?,
        eventType: EventType,
    ): Long = TSID.Factory.getTsid().toLong()

    override fun getEventTypes(): EnumSet<EventType> = EventTypeSets.INSERT_ONLY
}
