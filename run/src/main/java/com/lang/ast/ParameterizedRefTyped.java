package com.lang.ast;

import java.util.List;

import com.lang.util.Position;

public class ParameterizedRefTyped extends Typed {

	public String name;
	public List<Typed> typeArguments;

	public ParameterizedRefTyped() {
	}

	public ParameterizedRefTyped(String name, List<Typed> typeArguments, Position position) {
		super(position);
		this.name = name;
		this.typeArguments = typeArguments;
	}

	@Override
	public String getName() {
		StringBuilder sb = new StringBuilder(name);

		if (!typeArguments.isEmpty()) {
			sb.append("<");
			for (int i = 0; i < typeArguments.size(); i++) {
				if (i > 0)
					sb.append(", ");
				sb.append(typeArguments.get(i).getName());
			}
			sb.append(">");
		}

		return sb.toString();
	}
}