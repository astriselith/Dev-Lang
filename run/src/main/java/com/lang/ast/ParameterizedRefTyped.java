package com.lang.ast;

import java.util.List;

import com.lang.util.Position;

public class ParameterizedRefTyped extends Typed {

	public Identifier name;
	public List<Typed> typeArguments;

	public ParameterizedRefTyped() {
	}

	public ParameterizedRefTyped(Identifier name, List<Typed> typeArguments, Position position) {
		super(position);
		this.name = name;
		this.typeArguments = typeArguments;
	}

	@Override
	public String getName() {
		return name.source;
	}

	@Override
	public String getSignature() {
		StringBuilder sb = new StringBuilder(name.source);

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