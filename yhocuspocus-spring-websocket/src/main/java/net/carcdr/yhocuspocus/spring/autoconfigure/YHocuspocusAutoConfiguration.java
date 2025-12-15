package net.carcdr.yhocuspocus.spring.autoconfigure;

import net.carcdr.yhocuspocus.core.YHocuspocus;
import net.carcdr.yhocuspocus.extension.Extension;
import net.carcdr.yhocuspocus.extension.InMemoryDatabaseExtension;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Auto-configuration for YHocuspocus server.
 *
 * <p>Creates a {@link YHocuspocus} bean if one is not already defined.
 * Extensions can be provided as Spring beans and will be automatically
 * added to the server.</p>
 *
 * <p>If no extensions are configured, an {@link InMemoryDatabaseExtension}
 * is used by default for basic functionality.</p>
 *
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(YHocuspocusProperties.class)
public class YHocuspocusAutoConfiguration {

    /**
     * Creates a YHocuspocus server bean.
     *
     * @param properties configuration properties
     * @param extensions list of extensions from Spring context
     * @return configured YHocuspocus instance
     */
    @Bean
    @ConditionalOnMissingBean
    public YHocuspocus yHocuspocus(
            YHocuspocusProperties properties,
            List<Extension> extensions) {

        YHocuspocus.Builder builder = YHocuspocus.builder()
            .debounce(properties.getDebounce())
            .maxDebounce(properties.getMaxDebounce());

        // Add all extensions from Spring context
        extensions.forEach(builder::extension);

        // If no extensions configured, add default in-memory storage
        if (extensions.isEmpty()) {
            builder.extension(new InMemoryDatabaseExtension());
        }

        return builder.build();
    }
}
