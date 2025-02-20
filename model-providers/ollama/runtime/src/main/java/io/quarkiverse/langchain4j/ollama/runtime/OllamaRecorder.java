package io.quarkiverse.langchain4j.ollama.runtime;

import static io.quarkiverse.langchain4j.runtime.OptionalUtil.firstOrDefault;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.DisabledChatLanguageModel;
import dev.langchain4j.model.chat.DisabledStreamingChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.embedding.DisabledEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.quarkiverse.langchain4j.ollama.OllamaChatLanguageModel;
import io.quarkiverse.langchain4j.ollama.OllamaEmbeddingModel;
import io.quarkiverse.langchain4j.ollama.OllamaStreamingChatLanguageModel;
import io.quarkiverse.langchain4j.ollama.Options;
import io.quarkiverse.langchain4j.ollama.runtime.config.ChatModelConfig;
import io.quarkiverse.langchain4j.ollama.runtime.config.EmbeddingModelConfig;
import io.quarkiverse.langchain4j.ollama.runtime.config.LangChain4jOllamaConfig;
import io.quarkiverse.langchain4j.ollama.runtime.config.LangChain4jOllamaFixedRuntimeConfig;
import io.quarkiverse.langchain4j.runtime.NamedConfigUtil;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class OllamaRecorder {

    private static final String DEFAULT_BASE_URL = "http://localhost:11434";

    private static final TypeLiteral<Instance<ChatModelListener>> CHAT_MODEL_LISTENER_TYPE_LITERAL = new TypeLiteral<>() {
    };

    public Function<SyntheticCreationalContext<ChatLanguageModel>, ChatLanguageModel> chatModel(
            LangChain4jOllamaConfig runtimeConfig,
            LangChain4jOllamaFixedRuntimeConfig fixedRuntimeConfig, String configName) {
        LangChain4jOllamaConfig.OllamaConfig ollamaConfig = correspondingOllamaConfig(runtimeConfig, configName);
        LangChain4jOllamaFixedRuntimeConfig.OllamaConfig ollamaFixedConfig = correspondingOllamaFixedConfig(fixedRuntimeConfig,
                configName);

        if (ollamaConfig.enableIntegration()) {
            ChatModelConfig chatModelConfig = ollamaConfig.chatModel();

            Options.Builder optionsBuilder = Options.builder()
                    .temperature(chatModelConfig.temperature())
                    .topK(chatModelConfig.topK())
                    .topP(chatModelConfig.topP());

            if (chatModelConfig.numPredict().isPresent()) {
                optionsBuilder.numPredict(chatModelConfig.numPredict().getAsInt());
            }
            if (chatModelConfig.stop().isPresent()) {
                optionsBuilder.stop(chatModelConfig.stop().get());
            }
            if (chatModelConfig.seed().isPresent()) {
                optionsBuilder.seed(chatModelConfig.seed().get());
            }
            var builder = OllamaChatLanguageModel.builder()
                    .baseUrl(ollamaConfig.baseUrl().orElse(DEFAULT_BASE_URL))
                    .tlsConfigurationName(ollamaConfig.tlsConfigurationName().orElse(null))
                    .timeout(ollamaConfig.timeout().orElse(Duration.ofSeconds(10)))
                    .logRequests(firstOrDefault(false, chatModelConfig.logRequests(), ollamaConfig.logRequests()))
                    .logResponses(firstOrDefault(false, chatModelConfig.logResponses(), ollamaConfig.logResponses()))
                    .model(ollamaFixedConfig.chatModel().modelId())
                    .format(chatModelConfig.format().orElse(null))
                    .configName(NamedConfigUtil.isDefault(configName) ? null : configName)
                    .options(optionsBuilder.build());

            return new Function<>() {
                @Override
                public ChatLanguageModel apply(SyntheticCreationalContext<ChatLanguageModel> context) {
                    builder.listeners(context.getInjectedReference(CHAT_MODEL_LISTENER_TYPE_LITERAL).stream()
                            .collect(Collectors.toList()));
                    return builder.build();
                }
            };
        } else {
            return new Function<>() {
                @Override
                public ChatLanguageModel apply(SyntheticCreationalContext<ChatLanguageModel> context) {
                    return new DisabledChatLanguageModel();
                }
            };
        }
    }

    public Supplier<EmbeddingModel> embeddingModel(LangChain4jOllamaConfig runtimeConfig,
            LangChain4jOllamaFixedRuntimeConfig fixedRuntimeConfig, String configName) {
        LangChain4jOllamaConfig.OllamaConfig ollamaConfig = correspondingOllamaConfig(runtimeConfig, configName);
        LangChain4jOllamaFixedRuntimeConfig.OllamaConfig ollamaFixedConfig = correspondingOllamaFixedConfig(fixedRuntimeConfig,
                configName);

        if (ollamaConfig.enableIntegration()) {
            EmbeddingModelConfig embeddingModelConfig = ollamaConfig.embeddingModel();
            Options.Builder optionsBuilder = Options.builder()
                    .temperature(embeddingModelConfig.temperature())
                    .topK(embeddingModelConfig.topK())
                    .topP(embeddingModelConfig.topP())
                    .numPredict(embeddingModelConfig.numPredict());

            if (embeddingModelConfig.stop().isPresent()) {
                optionsBuilder.stop(embeddingModelConfig.stop().get());
            }

            var builder = OllamaEmbeddingModel.builder()
                    .baseUrl(ollamaConfig.baseUrl().orElse(DEFAULT_BASE_URL))
                    .tlsConfigurationName(ollamaConfig.tlsConfigurationName().orElse(null))
                    .timeout(ollamaConfig.timeout().orElse(Duration.ofSeconds(10)))
                    .model(ollamaFixedConfig.embeddingModel().modelId())
                    .logRequests(firstOrDefault(false, embeddingModelConfig.logRequests(), ollamaConfig.logRequests()))
                    .logResponses(firstOrDefault(false, embeddingModelConfig.logResponses(), ollamaConfig.logResponses()))
                    .configName(NamedConfigUtil.isDefault(configName) ? null : configName);

            return new Supplier<>() {
                @Override
                public EmbeddingModel get() {
                    return builder.build();
                }
            };
        } else {
            return new Supplier<>() {
                @Override
                public EmbeddingModel get() {
                    return new DisabledEmbeddingModel();
                }
            };
        }
    }

    public Supplier<StreamingChatLanguageModel> streamingChatModel(LangChain4jOllamaConfig runtimeConfig,
            LangChain4jOllamaFixedRuntimeConfig fixedRuntimeConfig, String configName) {
        LangChain4jOllamaConfig.OllamaConfig ollamaConfig = correspondingOllamaConfig(runtimeConfig, configName);
        LangChain4jOllamaFixedRuntimeConfig.OllamaConfig ollamaFixedConfig = correspondingOllamaFixedConfig(fixedRuntimeConfig,
                configName);

        if (ollamaConfig.enableIntegration()) {
            ChatModelConfig chatModelConfig = ollamaConfig.chatModel();

            Options.Builder optionsBuilder = Options.builder()
                    .temperature(chatModelConfig.temperature())
                    .topK(chatModelConfig.topK())
                    .topP(chatModelConfig.topP());

            if (chatModelConfig.numPredict().isPresent()) {
                optionsBuilder.numPredict(chatModelConfig.numPredict().getAsInt());
            }
            if (chatModelConfig.stop().isPresent()) {
                optionsBuilder.stop(chatModelConfig.stop().get());
            }
            if (chatModelConfig.seed().isPresent()) {
                optionsBuilder.seed(chatModelConfig.seed().get());
            }
            var builder = OllamaStreamingChatLanguageModel.builder()
                    .baseUrl(ollamaConfig.baseUrl().orElse(DEFAULT_BASE_URL))
                    .tlsConfigurationName(ollamaConfig.tlsConfigurationName().orElse(null))
                    .timeout(ollamaConfig.timeout().orElse(Duration.ofSeconds(10)))
                    .logRequests(firstOrDefault(false, chatModelConfig.logRequests(), ollamaConfig.logRequests()))
                    .logResponses(firstOrDefault(false, chatModelConfig.logResponses(), ollamaConfig.logResponses()))
                    .model(ollamaFixedConfig.chatModel().modelId())
                    .options(optionsBuilder.build())
                    .configName(NamedConfigUtil.isDefault(configName) ? null : configName);

            return new Supplier<>() {
                @Override
                public StreamingChatLanguageModel get() {
                    return builder.build();
                }
            };
        } else {
            return new Supplier<>() {
                @Override
                public StreamingChatLanguageModel get() {
                    return new DisabledStreamingChatLanguageModel();
                }
            };
        }
    }

    private LangChain4jOllamaConfig.OllamaConfig correspondingOllamaConfig(LangChain4jOllamaConfig runtimeConfig,
            String configName) {
        LangChain4jOllamaConfig.OllamaConfig ollamaConfig;
        if (NamedConfigUtil.isDefault(configName)) {
            ollamaConfig = runtimeConfig.defaultConfig();
        } else {
            ollamaConfig = runtimeConfig.namedConfig().get(configName);
        }
        return ollamaConfig;
    }

    private LangChain4jOllamaFixedRuntimeConfig.OllamaConfig correspondingOllamaFixedConfig(
            LangChain4jOllamaFixedRuntimeConfig runtimeConfig,
            String configName) {
        LangChain4jOllamaFixedRuntimeConfig.OllamaConfig ollamaConfig;
        if (NamedConfigUtil.isDefault(configName)) {
            ollamaConfig = runtimeConfig.defaultConfig();
        } else {
            ollamaConfig = runtimeConfig.namedConfig().get(configName);
        }
        return ollamaConfig;
    }
}
