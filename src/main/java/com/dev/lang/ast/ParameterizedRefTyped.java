package com.dev.lang.ast;

import com.dev.lang.util.Position;
import java.util.ArrayList;
import java.util.List;

public class ParameterizedRefTyped extends Typed {

	public final String name;
	public final List<Typed> typeArguments;

	public ParameterizedRefTyped(String name, List<Typed> typeArguments, Position position) {
		super(position);
		this.name = name;
		this.typeArguments = typeArguments != null ? typeArguments : new ArrayList<>();
	}

	@Override
	public String getName() {
		StringBuilder sb = new StringBuilder(name);

		if (!typeArguments.isEmpty()) {
			sb.append("<");
			for (int i = 0; i < typeArguments.size(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(typeArguments.get(i).getName());
			}
			sb.append(">");
		}

		return sb.toString();
	}
}