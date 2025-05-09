package org.quiltmc.enigma.api.translation.representation.entry;

import com.google.common.base.Preconditions;
import org.quiltmc.enigma.api.translation.TranslateResult;
import org.quiltmc.enigma.api.translation.Translator;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.representation.AccessFlags;
import org.quiltmc.enigma.api.translation.representation.MethodDescriptor;
import org.quiltmc.enigma.api.translation.representation.Signature;

import javax.annotation.Nonnull;

public class MethodDefEntry extends MethodEntry implements DefEntry<ClassEntry> {
	private final AccessFlags access;
	private final Signature signature;

	public MethodDefEntry(ClassEntry owner, String name, MethodDescriptor descriptor, Signature signature, AccessFlags access) {
		this(owner, name, descriptor, signature, access, null);
	}

	public MethodDefEntry(ClassEntry owner, String name, MethodDescriptor descriptor, Signature signature, AccessFlags access, String docs) {
		super(owner, name, descriptor, docs);
		Preconditions.checkNotNull(access, "Method access cannot be null");
		Preconditions.checkNotNull(signature, "Method signature cannot be null");
		this.access = access;
		this.signature = signature;
	}

	public static MethodDefEntry parse(ClassEntry owner, int access, String name, String desc, String signature) {
		return new MethodDefEntry(owner, name, new MethodDescriptor(desc), Signature.createSignature(signature), new AccessFlags(access), null);
	}

	@Override
	public AccessFlags getAccess() {
		return this.access;
	}

	public Signature getSignature() {
		return this.signature;
	}

	@Override
	protected TranslateResult<MethodDefEntry> extendedTranslate(Translator translator, @Nonnull EntryMapping mapping) {
		MethodDescriptor translatedDesc = translator.translate(this.descriptor);
		Signature translatedSignature = translator.translate(this.signature);
		String translatedName = mapping.targetName() != null ? mapping.targetName() : this.name;
		String docs = mapping.javadoc();
		return TranslateResult.of(
				mapping,
				new MethodDefEntry(this.parent, translatedName, translatedDesc, translatedSignature, this.access, docs)
		);
	}

	@Override
	public MethodDefEntry withName(String name) {
		return new MethodDefEntry(this.parent, name, this.descriptor, this.signature, this.access, this.javadocs);
	}

	@Override
	public MethodDefEntry withParent(ClassEntry parent) {
		return new MethodDefEntry(new ClassEntry(parent.getFullName()), this.name, this.descriptor, this.signature, this.access, this.javadocs);
	}
}
