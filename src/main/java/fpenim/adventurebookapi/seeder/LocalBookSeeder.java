package fpenim.adventurebookapi.seeder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

@Component
@Profile("local & seed")
public class LocalBookSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(LocalBookSeeder.class);

    private final PathMatchingResourcePatternResolver resolver;

    public LocalBookSeeder(ResourceLoader resourceLoader) {
        this.resolver = new PathMatchingResourcePatternResolver(resourceLoader);
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        Resource[] files =
                resolver.getResources("classpath:/seed/books/*.json");

        Arrays.sort(
                files,
                Comparator.comparing(Resource::getFilename)
        );

        if (files.length == 0) {
            throw new IllegalStateException(
                    "No seed files found in classpath:/seed/books/"
            );
        }

        for (Resource file : files) {
            log.info("Found book seed file: {}", file.getFilename());
        }
    }
}
