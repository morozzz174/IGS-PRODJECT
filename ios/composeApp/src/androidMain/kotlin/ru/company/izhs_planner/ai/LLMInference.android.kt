package ru.company.izhs_planner.ai

actual class LLMInference {
    private var initialized = false

    init {
        try {
            System.loadLibrary("llama_jni")
        } catch (e: UnsatisfiedLinkError) {
            android.util.Log.e("LLMInference", "Failed to load native library", e)
        }
    }

    private external fun nativeInit(modelPath: String): Boolean
    private external fun nativeGenerate(prompt: String, maxTokens: Int): String
    private external fun nativeReset()
    private external fun nativeRelease()

    actual fun init(modelPath: String): Boolean {
        if (initialized) return true
        initialized = nativeInit(modelPath)
        return initialized
    }

    actual fun generate(prompt: String, maxTokens: Int): String {
        if (!initialized) throw IllegalStateException("LLM model not initialized")
        return nativeGenerate(prompt, maxTokens)
    }

    actual fun reset() {
        if (initialized) nativeReset()
    }

    actual fun release() {
        if (initialized) {
            nativeRelease()
            initialized = false
        }
    }

    actual fun isInitialized(): Boolean = initialized
}
