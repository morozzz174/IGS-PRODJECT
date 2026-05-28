#include "llama.h"
#include <jni.h>
#include <string>
#include <vector>
#include <cstring>

static llama_model *g_model = nullptr;
static llama_context *g_ctx = nullptr;
static int g_n_past = 0;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_ru_company_izhs_planner_ai_LLMInference_nativeInit(
    JNIEnv *env, jobject /*thiz*/, jstring model_path) {

    if (g_model && g_ctx) {
        return JNI_TRUE;
    }

    llama_backend_init();

    const char *path = env->GetStringUTFChars(model_path, nullptr);

    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0;

    g_model = llama_load_model_from_file(path, model_params);
    env->ReleaseStringUTFChars(model_path, path);

    if (!g_model) {
        return JNI_FALSE;
    }

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 4096;
    ctx_params.n_batch = 512;
    ctx_params.n_threads = 4;
    ctx_params.n_threads_batch = 4;

    g_ctx = llama_new_context_with_model(g_model, ctx_params);
    if (!g_ctx) {
        llama_free_model(g_model);
        g_model = nullptr;
        return JNI_FALSE;
    }

    g_n_past = 0;
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL
Java_ru_company_izhs_planner_ai_LLMInference_nativeGenerate(
    JNIEnv *env, jobject /*thiz*/, jstring prompt, jint max_tokens) {

    if (!g_model || !g_ctx) {
        return env->NewStringUTF("");
    }

    const char *prompt_str = env->GetStringUTFChars(prompt, nullptr);

    int n_tokens = llama_tokenize(
        g_model, prompt_str, -1, nullptr, 0, true, false
    );
    if (n_tokens <= 0) {
        env->ReleaseStringUTFChars(prompt, prompt_str);
        return env->NewStringUTF("");
    }

    std::vector<llama_token> tokens(n_tokens);
    llama_tokenize(
        g_model, prompt_str, -1, tokens.data(), tokens.size(), true, false
    );
    env->ReleaseStringUTFChars(prompt, prompt_str);

    llama_decode(g_ctx, llama_batch_get_one(tokens.data(), tokens.size(), g_n_past, 0));
    g_n_past += (int)tokens.size();

    std::string result;
    result.reserve(max_tokens * 4);

    for (int i = 0; i < max_tokens; i++) {
        llama_token id = llama_sample_token(g_ctx, nullptr, nullptr, 0);

        if (id == llama_token_eos(g_model)) {
            break;
        }

        char buf[128];
        int n = llama_token_to_piece(g_model, id, buf, sizeof(buf), 0, true);
        if (n > 0) {
            result.append(buf, (size_t)n);
        }

        llama_token next = id;
        llama_decode(g_ctx, llama_batch_get_one(&next, 1, g_n_past, 0));
        g_n_past++;
    }

    return env->NewStringUTF(result.c_str());
}

JNIEXPORT void JNICALL
Java_ru_company_izhs_planner_ai_LLMInference_nativeReset(
    JNIEnv * /*env*/, jobject /*thiz*/) {
    if (g_ctx) {
        llama_kv_cache_clear(g_ctx);
    }
    g_n_past = 0;
}

JNIEXPORT void JNICALL
Java_ru_company_izhs_planner_ai_LLMInference_nativeRelease(
    JNIEnv * /*env*/, jobject /*thiz*/) {
    if (g_ctx) {
        llama_free(g_ctx);
        g_ctx = nullptr;
    }
    if (g_model) {
        llama_free_model(g_model);
        g_model = nullptr;
    }
    llama_backend_free();
    g_n_past = 0;
}

} // extern "C"
