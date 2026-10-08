package ac.at.uibk.dps.dapr.smartfactory.api

import org.apache.fory.Fory
import org.apache.fory.ThreadSafeFory
import org.apache.fory.config.Language

object ForyConfig {
    val fory: ThreadSafeFory =
        Fory.builder().withLanguage(Language.XLANG).withRefTracking(true).buildThreadSafeFory().apply {
        register(EmptyRequest::class.java)
        register(StatisticsRequest::class.java)
        register(MessageProcessingRequest::class.java)
        register(PhotoScanResponse::class.java)
        register(PhotoScanRequest::class.java)
        }
}