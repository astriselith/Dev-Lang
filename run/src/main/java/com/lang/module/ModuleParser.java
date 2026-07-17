package com.lang.module;

import com.lang.json.Json;
import com.lang.json.JsonArray;
import com.lang.json.JsonObject;
import com.lang.json.JsonParser;
import com.lang.token.TokenStream;
import com.lang.unit.CompilationUnit;
import com.lang.util.Positioned;
import java.util.ArrayList;
import java.util.List;

public final class ModuleParser {

	private static final String TAG = "MODULE";
	private static final String UNKNOWN = "unknown";
	private static final String DEFAULT_VERSION = "0.0.0";
	private static final String LATEST = "latest";

	private final TokenStream stream;
	private final CompilationUnit unit;

	public ModuleParser(TokenStream stream, CompilationUnit unit) {
		this.stream = stream;
		this.unit = unit;
	}

	public Module parse() {
		try {
			JsonParser parser = new JsonParser(stream, unit);
			Json<?> json = parser.parse();

			if (json.isNull()) {
				unit.addError(unit.error(TAG, "Invalid module-info: null", (Positioned) null));
				return Module.empty(unit);
			}

			if (!json.isObject()) {
				unit.addError(unit.error(TAG, "Invalid module-info: expected object", (Positioned) null));
				return Module.empty(unit);
			}

			JsonObject obj = json.asObject();

			String packageName = obj.optString("package", UNKNOWN);
			String artifactId = obj.optString("artifact", UNKNOWN);
			String version = obj.optString("version", DEFAULT_VERSION);

			List<Module> dependencies = new ArrayList<>();

			JsonArray depsArray = obj.optArray("dependencies");
			if (depsArray != null) {
				for (Json<?> depJson : depsArray.getElements()) {
					if (!depJson.isString()) {
						unit.addError(unit.error(TAG, "Invalid dependency: expected string, got " + depJson.getTypeName(), (Positioned) null));
						continue;
					}

					String str = depJson.asString().getValue();
					String[] parts = str.split(":");

					switch (parts.length) {
					case 3:
						dependencies.add(Module.from(parts[0], parts[1], parts[2]));
						break;
					case 2:
						dependencies.add(Module.from(parts[0], parts[1], LATEST));
						break;
					default:
						unit.addError(unit.error(TAG, "Invalid dependency format: " + str + " (expected pkg:artifact:version)", (Positioned) null));
						break;
					}
				}
			}

			return new Module(packageName, artifactId, version, dependencies, unit, null);

		} catch (Exception e) {
			unit.addError(unit.error(TAG, "Error parsing module-info: " + e.getMessage(), (Positioned) null, e));
			return Module.empty(unit);
		}
	}
}