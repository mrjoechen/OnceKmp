package space.joechen.oncekmpsample

import space.joechen.Amount
import space.joechen.Once
import space.joechen.OnceTimeUnit

class OnceKmpSample(
    private val whatsNewTag: String = DEFAULT_WHATS_NEW_TAG,
) {
    fun shouldShowWhatsNewThisVersion(): Boolean {
        return !Once.beenDone(Once.THIS_APP_VERSION, whatsNewTag)
    }

    fun markWhatsNewShown() {
        Once.markDone(whatsNewTag)
    }

    fun shouldSyncNow(syncTag: String = DEFAULT_SYNC_TAG): Boolean {
        return !Once.beenDone(OnceTimeUnit.HOURS, 1, syncTag)
    }

    fun markSynced(syncTag: String = DEFAULT_SYNC_TAG) {
        Once.markDone(syncTag)
    }

    fun trackCoreActionAndShouldPromptRating(
        actionTag: String = DEFAULT_ACTION_TAG,
        promptAtCount: Int = 3,
    ): Boolean {
        Once.markDone(actionTag)
        return Once.beenDone(actionTag, Amount.exactly(promptAtCount))
    }

    companion object {
        const val DEFAULT_WHATS_NEW_TAG = "sample.show_whats_new"
        const val DEFAULT_SYNC_TAG = "sample.sync_data"
        const val DEFAULT_ACTION_TAG = "sample.core_action"
    }
}
