package gg.gianluca.giantags;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Declares runtime dependencies that Paper downloads and injects into the
 * plugin classloader before the plugin class is loaded.  This removes the
 * need to shade TriumphGUI, HikariCP, and the MariaDB driver into the JAR.
 */
@SuppressWarnings("UnstableApiUsage")
public final class GianTagsLoader implements PluginLoader {

    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();

        // TriumphGUI is published to repo.triumphteam.dev (not Maven Central)
        resolver.addRepository(Objects.requireNonNull(new RemoteRepository.Builder(
                "triumphteam",
                "default",
                "https://repo.triumphteam.dev/snapshots/").build()));

        // Use Paper's approved Maven Central mirror for HikariCP, the MariaDB
        // driver, and any transitive deps (e.g. Kotlin stdlib via TriumphGUI).
        // MAVEN_CENTRAL_DEFAULT_MIRROR is a String URL in this API version, so
        // wrap it in a RemoteRepository.Builder before passing to addRepository().
        resolver.addRepository(Objects.requireNonNull(new RemoteRepository.Builder(
                "central", "default", MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR).build()));

        // TriumphGUI (includes transitive Kotlin stdlib)
        resolver.addDependency(new Dependency(
                new DefaultArtifact("dev.triumphteam:triumph-gui:3.1.13"), null));

        // HikariCP — connection pooling for SQL storage
        resolver.addDependency(new Dependency(
                new DefaultArtifact("com.zaxxer:HikariCP:5.1.0"), null));

        // MariaDB JDBC driver — supports both MySQL and MariaDB
        resolver.addDependency(new Dependency(
                new DefaultArtifact("org.mariadb.jdbc:mariadb-java-client:3.4.1"), null));

        classpathBuilder.addLibrary(resolver);
    }
}
