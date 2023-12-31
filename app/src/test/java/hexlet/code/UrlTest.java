package hexlet.code;

import hexlet.code.model.Url;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;
import io.javalin.http.NotFoundResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.MockResponse;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public final class UrlTest {

    private static MockWebServer mockServer;

    private static Javalin app;

    private static Path getAbsolutePath(String filePath) {
        return Paths.get(filePath).toAbsolutePath().normalize();
    }

    private static String getDataFromFile(Path absoluteFilePath) throws Exception {
        return Files.readString(absoluteFilePath).trim();
    }

    @BeforeAll
    public static void beforeAll() throws Exception {
        mockServer = new MockWebServer();

        String pathToFile = "./src/test/resources/exampleDomain.html";

        String examFile = getDataFromFile(getAbsolutePath(pathToFile));

        MockResponse responseExam = new MockResponse()
                .setBody(examFile);

        mockServer.enqueue(responseExam);
        mockServer.start();
    }

    @AfterAll
    public static void afterAll() throws IOException {
        app.stop();
        mockServer.shutdown();
    }

    @BeforeEach
    public void setUp() throws Exception {
        app = App.getApp();
    }

    @Test
    void testMainPage() throws Exception {

        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/");
            assertThat(response.code()).isEqualTo(HttpStatus.SC_OK);
        });
    }

    @Test
    void testUrlPage() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            var url = new Url("http://www.example.com", Timestamp.valueOf(LocalDateTime.now()));
            UrlRepository.save(url);

            var response = client.get("/urls");
            assertThat(response.code()).isEqualTo(HttpStatus.SC_OK);
            assertThat(response.body().string()).contains("http://www.example.com");
        });
    }

    @Test
    public void testUrlsPage() throws Exception {
        var url = new Url("http://www.example.com", Timestamp.valueOf(LocalDateTime.now()));
        UrlRepository.save(url);
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/urls/" + url.getId());
            assertThat(response.code()).isEqualTo(HttpStatus.SC_OK);
            assertThat(response.body().string()).contains("http://www.example.com");
        });
    }

    @Test
    public void testCreateUrl() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            var requestBody = "url=http://www.example.com";
            var response = client.post("/urls", requestBody);
            assertThat(response.code()).isEqualTo(HttpStatus.SC_OK);
            var url = UrlRepository.find(1L)
                    .orElseThrow(() -> new NotFoundResponse("Url not found"));

            String id = String.valueOf(url.getId());
            String name = url.getName();

            assertThat(response.body().string()).contains(id, name);
        });
        assertThat(UrlRepository.getEntities()).hasSize(1);
    }

    @Test
    void testUrlNotFound() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/urls/999999");
            assertThat(response.code()).isEqualTo(HttpStatus.SC_NOT_FOUND);
        });
    }

    @Test
    public void testCreateCheck() throws Exception {
        String urlForCheck = mockServer.url("http://www.example.com").toString();

        var url = new Url(urlForCheck, Timestamp.valueOf(LocalDateTime.now()));
        UrlRepository.save(url);

        JavalinTest.test(app, (server, client) -> {
            var response = client.post("/urls/1/checks");
            assertThat(response.code()).isEqualTo(HttpStatus.SC_OK);

            var urlCheck = UrlCheckRepository.getLastCheck(1L);

            String id = String.valueOf(urlCheck.getId());
            String title = urlCheck.getTitle();
            String statusCode = String.valueOf(urlCheck.getStatusCode());

            assertThat(response.body().string()).contains(id, title, statusCode);
        });

        assertThat(UrlCheckRepository.getEntities(1L)).hasSize(1);
    }

    @Test
    public void testEmptyCheck() throws Exception {
        String urlForCheck = mockServer.url("http://www.empty.com").toString();

        var url = new Url(urlForCheck, Timestamp.valueOf(LocalDateTime.now()));
        UrlRepository.save(url);

        JavalinTest.test(app, (server, client) -> {
            var response = client.post("/urls/1/checks");
            assertThat(response.code()).isEqualTo(HttpStatus.SC_OK);
            assertThat(response.body().string()).doesNotContain("Example Domain");
        });

        assertThat(UrlCheckRepository.getEntities(1L)).hasSize(1);
    }

    @Test
    public void wrongUrl() throws Exception {
        String urlForCheck = mockServer.url("http://www.empty.commar").toString();

        var url = new Url(urlForCheck, Timestamp.valueOf(LocalDateTime.now()));
        UrlRepository.save(url);

        JavalinTest.test(app, (server, client) -> {
            var response = client.post("/urls/1/checks");
            assertThat(response.code()).isEqualTo(HttpStatus.SC_OK);
            assertThat(response.body().string()).doesNotContain("Example Domain");
        });

        assertThat(UrlCheckRepository.getEntities(1L)).hasSize(0);
    }
}
