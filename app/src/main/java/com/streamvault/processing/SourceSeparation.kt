package com.streamvault.processing

import android.content.Context
import android.net.Uri
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Provider contract: cancellation must stop decoding/inference/upload; original URI is read-only. */
interface SourceSeparator {
    val name: String
    suspend fun availability(context: Context): Availability
    fun separate(context: Context, source: Uri, outputDirectory: File): Flow<SeparationEvent>
}
sealed interface Availability {
    data object Ready : Availability
    data class Unavailable(val reason: String) : Availability
}
sealed interface SeparationEvent {
    data class Progress(val percent: Int, val stage: String) : SeparationEvent
    /** Both files must exist, use a supported real container/extension and never equal the source. */
    data class Complete(val vocals: File, val instrumental: File) : SeparationEvent
    data class Failed(val message: String) : SeparationEvent
}
class SeparationRegistry {
    var provider: SourceSeparator = NoModelInstalled()
        private set
    fun register(provider: SourceSeparator) { this.provider = provider }
}
private class NoModelInstalled : SourceSeparator {
    override val name = "Sin modelo instalado"
    override suspend fun availability(context: Context) = Availability.Unavailable(
        "La separación de voz necesita un modelo compatible o un servidor. Esta versión no incluye un modelo; no envía tus audios ni simula un instrumental."
    )
    override fun separate(context: Context, source: Uri, outputDirectory: File) = flow<SeparationEvent> {
        emit(SeparationEvent.Failed("Instala y registra un proveedor de separación antes de procesar."))
    }
}
