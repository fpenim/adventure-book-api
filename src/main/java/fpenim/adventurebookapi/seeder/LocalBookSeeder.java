package fpenim.adventurebookapi.seeder;

import fpenim.adventurebookapi.book.BookImportService;
import fpenim.adventurebookapi.book.dto.BookImportRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
@Profile("local & seed")
public class LocalBookSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(LocalBookSeeder.class);

    private final PathMatchingResourcePatternResolver resolver;

    private final JsonMapper jsonMapper;

    private final BookImportService bookImportService;

    public LocalBookSeeder(ResourceLoader resourceLoader, JsonMapper jsonMapper, BookImportService bookImportService) {
        this.resolver = new PathMatchingResourcePatternResolver(resourceLoader);
        this.jsonMapper = jsonMapper;
        this.bookImportService = bookImportService;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        Resource[] files = resolver.getResources("classpath:/seed/books/*.json");

        Arrays.sort(files, Comparator.comparing(Resource::getFilename));

        if (files.length == 0) {
            throw new IllegalStateException("No seed files found in classpath:/seed/books/");
        }

        for (Resource file : files) {
            log.info("Found book seed file '{}", file.getFilename());

            BookImportRequest book = readBook(file);

            bookImportService.importBook(book);
        }
    }

    private BookImportRequest readBook(Resource file) {
        try (InputStream input = file.getInputStream()) {
            BookImportRequest book = jsonMapper.readValue(input, BookImportRequest.class);

            if (book == null) {
                throw new IllegalStateException("Seed file contains null: " + file.getFilename());
            }

            return book;
        } catch (IOException | JacksonException exception) {
            throw new IllegalStateException("Could not read book seed file: " + file.getFilename(), exception);
        }
    }
}
