package org.quiltmc.enigma;

import org.quiltmc.enigma.api.analysis.EntryReference;
import org.quiltmc.enigma.api.class_provider.CachingClassProvider;
import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.quiltmc.enigma.api.class_provider.JarClassProvider;
import org.quiltmc.enigma.api.source.Decompiler;
import org.quiltmc.enigma.api.service.DecompilerService;
import org.quiltmc.enigma.api.source.Source;
import org.quiltmc.enigma.api.source.SourceIndex;
import org.quiltmc.enigma.api.source.SourceSettings;
import org.quiltmc.enigma.api.source.Token;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.util.Pair;
import org.tinylog.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TokenChecker {
	private static final Map<Pair<DecompilerService, Path>, Set<String>> ALL_SHOWN_FILES = new HashMap<>();

	private final Decompiler decompiler;
	private final Set<String> shownFiles;

	protected TokenChecker(Path path, DecompilerService decompilerService) throws IOException {
		this(path, decompilerService, new CachingClassProvider(new JarClassProvider(path)));
	}

	protected TokenChecker(Path path, DecompilerService decompilerService, ClassProvider classProvider) {
		this.decompiler = decompilerService.create(classProvider, new SourceSettings(false, false));
		this.shownFiles = ALL_SHOWN_FILES.computeIfAbsent(new Pair<>(decompilerService, path), p -> new HashSet<>());
	}

	protected String getDeclarationToken(Entry<?> entry) {
		// decompile the class
		Source source = this.decompiler.getUndocumentedSource(entry.getTopLevelClass().getFullName());
		// DEBUG
		// this.createDebugFile(source, entry.getTopLevelClass());
		String string = source.asString();
		SourceIndex index = source.index();

		// get the token value
		Token token = index.getDeclarationToken(entry);
		if (token == null) {
			return null;
		}

		return string.substring(token.start, token.end);
	}

	@SuppressWarnings("unchecked")
	protected Collection<String> getReferenceTokens(EntryReference<? extends Entry<?>, ? extends Entry<?>> reference) {
		// decompile the class
		Source source = this.decompiler.getUndocumentedSource(reference.context.getTopLevelClass().getFullName());
		String string = source.asString();
		SourceIndex index = source.index();
		// DEBUG
		// this.createDebugFile(source, reference.context.getTopLevelClass());

		// get the token values
		List<String> values = new ArrayList<>();
		for (Token token : index.getReferenceTokens((EntryReference<Entry<?>, Entry<?>>) reference)) {
			values.add(string.substring(token.start, token.end));
		}

		return values;
	}

	private void createDebugFile(Source source, ClassEntry classEntry) {
		if (!this.shownFiles.add(classEntry.getFullName())) {
			return;
		}

		try {
			String name = classEntry.getContextualName();
			Path path = Files.createTempFile("class-" + name.replace("$", "_") + "-", ".html");
			Files.writeString(path, SourceTestUtil.toHtml(source, name));
			Logger.info("Debug file created: {} ({})", path.toUri(), this.decompiler.getClass().getCanonicalName());
		} catch (Exception e) {
			Logger.error(e, "Failed to create debug source file for {}", classEntry);
		}
	}
}
