package ac.at.uibk.dps.dapr.smartfactory.actors.assemblyController

import ac.at.uibk.dps.dapr.smartfactory.api.PhotoScanRequest
import ac.at.uibk.dps.dapr.smartfactory.services.Services
import ac.at.uibk.dps.dapr.smartfactory.utils.Utils
import io.dapr.actors.ActorId
import io.dapr.actors.runtime.AbstractActor
import io.dapr.actors.runtime.ActorRuntimeContext
import io.dapr.client.DaprClientBuilder

class AssemblyControllerActorImpl(
  runtimeContext: ActorRuntimeContext<AssemblyControllerActorImpl>,
  id: ActorId,
) : AbstractActor(runtimeContext, id), AssemblyControllerActor {

  enum class State {
    DETECTING_START,
    CAPTURE_PHOTO,
    SCAN_PHOTO,
    ERROR,
    DETECTING_END,
    UNLOADING,
    JOB_DONE,
  }

  private var waitingParts: Long = 0

  private var state: State = State.DETECTING_START

  private val daprClient = DaprClientBuilder().build()

  // Measurement vars
  var firstDetection = true

  private fun transition(targetState: State, data: Any? = null) {
    when (targetState) {
      State.CAPTURE_PHOTO -> {
        if (state == State.DETECTING_START) {
          waitingParts -= 1
          state = State.CAPTURE_PHOTO
          enterCapturePhoto()
        }
      }

      State.SCAN_PHOTO -> {
        if (state == State.CAPTURE_PHOTO) {
          state = State.SCAN_PHOTO
          enterScanPhoto(data as ByteArray)
        }
      }

      State.DETECTING_END -> {
        if (state == State.SCAN_PHOTO) state = State.DETECTING_END
      }

      State.ERROR -> {
        if (state == State.SCAN_PHOTO) {
          state = State.ERROR
          enterError()
        }
      }

      State.DETECTING_START -> {
        if (state == State.ERROR || state == State.UNLOADING) {
          state = State.DETECTING_START
          enterDetectingStart()
        }
      }

      State.UNLOADING -> {
        if (state == State.DETECTING_END) state = State.UNLOADING
      }

      State.JOB_DONE -> {
        state = State.JOB_DONE
      }
    }
  }

  private fun enterDetectingStart() {
    if (waitingParts > 0) {
      transition(State.CAPTURE_PHOTO)
    }
  }

  private fun enterCapturePhoto() {
    // Invoking photo capture service
    Services.takePhoto().subscribe()
  }

  private fun enterScanPhoto(photoData: ByteArray) {
    // Invoking photo scan service
    Services.scanPhoto(PhotoScanRequest(photoData)).subscribe()
  }

  private fun enterError() {
    // Raise eProcessMessage
    Utils.publishEvent(
        daprClient,
        "pubsub",
        "eProcessMessage",
        mutableMapOf("msg" to "Assembly error: Invalid object detected"),
      )
      .subscribe()

    Services.discardObject().subscribe()
  }

  override fun initialize() {
    enterDetectingStart()
  }

  override fun detectedAtStart() {
    waitingParts = if (waitingParts == Long.MAX_VALUE) Long.MAX_VALUE else waitingParts + 1

    // Checking if first detection
    if (firstDetection) {
      // Emitting event to begin production time measurement
      Utils.publishEvent(daprClient, "pubsub", "eProductionStarted", mutableMapOf()).subscribe()
      firstDetection = false
    }

    transition(State.CAPTURE_PHOTO)
  }

  override fun processCapturedPhoto(photoData: ByteArray) {
    transition(State.SCAN_PHOTO, photoData)
  }

  override fun processPhotoScan(scanStatus: Boolean) {
    // Raising eScanned
    Utils.publishEvent(daprClient, "pubsub", "eScanned", mutableMapOf<String, Any?>()).subscribe()

    if (scanStatus) {
      // Raise eObjectValid
      Utils.publishEvent(daprClient, "pubsub", "eObjectValid", mutableMapOf<String, Any?>())
        .subscribe()

      transition(State.DETECTING_END)
    } else transition(State.ERROR)
  }

  override fun objectDiscarded() {
    transition(State.DETECTING_START)
  }

  override fun detectedAtEnd() {
    // Raising eStartUnload
    Utils.publishEvent(daprClient, "pubsub", "eStartUnload", mutableMapOf<String, Any?>())
      .subscribe()

    transition(State.UNLOADING)
  }

  override fun processPickup() {
    if (state == State.UNLOADING) transition(State.DETECTING_START)
  }

  override fun markJobDone() {
    transition(State.JOB_DONE)
  }
}
