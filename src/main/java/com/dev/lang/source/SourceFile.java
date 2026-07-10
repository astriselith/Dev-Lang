package com.dev.lang.source;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class SourceFile extends SourceStream {

	public SourceFile(Path path) throws IOException {
		super(Files.newInputStream(path), Files.size(path));
	}

	public SourceFile(InputStream inputStream, long length) {
		super(inputStream, length);
	}

	public SourceFile(InputStream inputStream) {
		super(inputStream);
	}
}