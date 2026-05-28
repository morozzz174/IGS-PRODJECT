package ru.company.izhs_planner.ai

object LLMInference {
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

    fun init(modelPath: String): Boolean {
        if (initialized) return true
        initialized = nativeInit(modelPath)
        return initialized
    }

    fun generate(prompt: String, maxTokens: Int = 2048): String {
        if (!initialized) {
            throw IllegalStateException("LLM model not initialized. Call init() first.")
        }
        return nativeGenerate(prompt, maxTokens)
    }

    fun reset() {
        if (initialized) {
            nativeReset()
        }
    }

    fun release() {
        if (initialized) {
            nativeRelease()
            initialized = false
        }
    }

    fun isInitialized(): Boolean = initialized
}
