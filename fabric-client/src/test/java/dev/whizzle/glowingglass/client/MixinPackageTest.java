package dev.whizzle.glowingglass.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mixin refuses to load any class from a mixin package that isn't a listed mixin; the game then fails to load its
 * resources (black screen; Shulker Styles client 0.2.0). Every file in the package must be in glowingglass.mixins.json.
 */
class MixinPackageTest {
	@Test void onlyListedMixinsInTheMixinPackage() throws IOException {
		// Gradle runs tests in fabric-client/; a plain-JUnit build elsewhere passes the folder in.
		Path base = Path.of(System.getProperty("glowingglass.client.dir", ""));
		String config = Files.readString(base.resolve("src/main/resources/glowingglass.mixins.json"));
		List<String> names;
		try (Stream<Path> files = Files.list(base.resolve("src/main/java/dev/whizzle/glowingglass/client/mixin"))) {
			names = files.map(f -> f.getFileName().toString().replace(".java", "")).toList();
		}
		assertFalse(names.isEmpty());
		for (String n : names) assertTrue(config.contains("\"" + n + "\""), n + " is in the mixin package but isn't a listed mixin");
	}
}
