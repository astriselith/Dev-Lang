package com.lang.tools;

import com.lang.ast.*;
import java.util.List;

public class Printer implements ExprVisitor<String>, StmtVisitor<String> {
	private int indent = 0;

	public String print(List<? extends Node> nodes) {
		StringBuilder sb = new StringBuilder();
		for (Node node : nodes) {
			sb.append(print(node));
		}
		return sb.toString();
	}

	public String print(Node node) {
		indent = 0;
		if (node instanceof Expr) {
			return ((Expr) node).accept(this);
		} else if (node instanceof Stmt) {
			return ((Stmt) node).accept(this);
		}
		return "";
	}

	private String indent() {
		if (indent == 0)
			return "";
		int dashes = 3 + (indent - 1) * 4;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < dashes; i++) {
			sb.append("-");
		}
		return sb.toString() + " ";
	}

	@Override
	public String visitLiteralExpr(LiteralExpr expr) {
		if (expr.value == null)
			return "null";
		return expr.value;
	}

	@Override
	public String visitRefExpr(RefExpr expr) {
		return expr.name;
	}

	@Override
	public String visitParenthesisExpr(ParenthesisExpr expr) {
		return "(" + expr.inner.accept(this) + ")";
	}

	@Override
	public String visitAssignExpr(AssignExpr expr) {
		return expr.target.accept(this) + " " + expr.operator + " " + expr.value.accept(this);
	}

	@Override
	public String visitBinaryExpr(BinaryExpr expr) {
		return expr.left.accept(this) + " " + expr.operator + " " + expr.right.accept(this);
	}

	@Override
	public String visitUnaryExpr(UnaryExpr expr) {
		return expr.operator + expr.operand.accept(this);
	}

	@Override
	public String visitTernaryExpr(TernaryExpr expr) {
		return expr.condition.accept(this) + " ? " + expr.thenExpr.accept(this) + " : " + expr.elseExpr.accept(this);
	}

	@Override
	public String visitCallExpr(CallExpr expr) {
		StringBuilder sb = new StringBuilder();
		sb.append(expr.callee.accept(this));

		if (!expr.typeArguments.isEmpty()) {
			sb.append("<");
			for (int i = 0; i < expr.typeArguments.size(); i++) {
				if (i > 0)
					sb.append(", ");
				sb.append(expr.typeArguments.get(i).getName());
			}
			sb.append(">");
		}

		sb.append("(");
		for (int i = 0; i < expr.arguments.size(); i++) {
			if (i > 0)
				sb.append(", ");
			sb.append(expr.arguments.get(i).accept(this));
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	public String visitIndexAccessExpr(IndexAccessExpr expr) {
		return expr.target.accept(this) + "[" + expr.index.accept(this) + "]";
	}

	@Override
	public String visitMemberAccessExpr(MemberAccessExpr expr) {
		return expr.object.accept(this) + "." + expr.name;
	}

	@Override
	public String visitVarDeclStmt(VarDeclStmt stmt) {
		StringBuilder sb = new StringBuilder();

		sb.append("var ").append(stmt.name);
		if (stmt.type != null) {
			sb.append(": ").append(stmt.type.getName());
		}
		if (stmt.value != null) {
			sb.append(" = ").append(stmt.value.accept(this));
		}
		sb.append(";");
		return sb.toString();
	}

	@Override
	public String visitFunDeclStmt(FunDeclStmt stmt) {
		StringBuilder sb = new StringBuilder();

		sb.append("fun ").append(stmt.name);

		if (stmt.typeParameters != null) {
			sb.append("<");
			for (int i = 0; i < stmt.typeParameters.size(); i++) {
				if (i > 0)
					sb.append(", ");
				sb.append(stmt.typeParameters.get(i).accept(this));
			}
			sb.append(">");
		}

		sb.append("(");
		for (int i = 0; i < stmt.parameters.size(); i++) {
			if (i > 0)
				sb.append(", ");
			ParamDeclStmt param = stmt.parameters.get(i);
			sb.append(param.accept(this));
		}
		sb.append(")");

		if (stmt.returnType != null) {
			sb.append(": ").append(stmt.returnType.getName());
		}

		if (stmt.body != null) {
			sb.append(" ");
			sb.append(stmt.body.accept(this));
		} else {
			sb.append(";");
		}
		return sb.toString();
	}

	@Override
	public String visitParamDeclStmt(ParamDeclStmt stmt) {
		if (stmt.type != null) {
			return stmt.name + ": " + stmt.type.getName();
		}
		return stmt.name;
	}

	@Override
	public String visitTypeParamDeclStmt(TypeParamDeclStmt stmt) {
		StringBuilder sb = new StringBuilder(stmt.name);

		if (stmt.superclasses != null && !stmt.superclasses.isEmpty()) {
			sb.append(" : ");
			sb.append(String.join(" & ", stmt.superclasses.stream()
					.map(Typed::getName)
					.toArray(String[]::new)));
		}

		return sb.toString();
	}

	@Override
	public String visitClassDeclStmt(ClassDeclStmt stmt) {
		StringBuilder sb = new StringBuilder();

		sb.append("class ").append(stmt.name);

		if (stmt.typeParameters != null) {
			sb.append("<");
			sb.append(String.join(", ", stmt.typeParameters.stream()
					.map(p -> p.accept(this))
					.toArray(String[]::new)));
			sb.append(">");
		}

		if (stmt.superclasses != null && !stmt.superclasses.isEmpty()) {
			sb.append(" : ");

			sb.append(String.join(" & ", stmt.superclasses.stream()
					.map(Typed::getName)
					.toArray(String[]::new)));
		}

		sb.append(" ");

		if (stmt.funs.isEmpty() && stmt.vars.isEmpty()) {
			sb.append("{}");
		} else {
			sb.append("{\n");
			indent++;

			for (VarDeclStmt var : stmt.vars) {
				if (var != null) {
					sb.append(indent()).append(var.accept(this)).append("\n");
				}
			}

			for (FunDeclStmt fun : stmt.funs) {
				if (fun != null) {
					sb.append(indent()).append(fun.accept(this)).append("\n");
				}
			}

			indent--;
			sb.append(indent()).append("}");
		}
		return sb.toString();
	}

	@Override
	public String visitLetDeclStmt(LetDeclStmt stmt) {
		StringBuilder sb = new StringBuilder();
		sb.append("let ").append(stmt.name);
		if (stmt.type != null) {
			sb.append(": ").append(stmt.type.getName());
		}
		if (stmt.value != null) {
			sb.append(" = ").append(stmt.value.accept(this));
		}
		sb.append(";");
		return sb.toString();
	}

	@Override
	public String visitIfStmt(IfStmt stmt) {
		StringBuilder sb = new StringBuilder();
		sb.append("if (").append(stmt.condition.accept(this)).append(") ");
		sb.append(stmt.thenBranch.accept(this));
		if (stmt.elseBranch != null && !stmt.elseBranch.statements.isEmpty()) {
			sb.append(" else ");
			sb.append(stmt.elseBranch.accept(this));
		}
		return sb.toString();
	}

	@Override
	public String visitWhileStmt(WhileStmt stmt) {
		StringBuilder sb = new StringBuilder();
		sb.append("while (").append(stmt.condition.accept(this)).append(") ");
		sb.append(stmt.body.accept(this));
		return sb.toString();
	}

	@Override
	public String visitReturnStmt(ReturnStmt stmt) {
		if (stmt.value != null) {
			return "return " + stmt.value.accept(this) + ";";
		}
		return "return;";
	}

	@Override
	public String visitBreakStmt(BreakStmt stmt) {
		return "break;";
	}

	@Override
	public String visitContinueStmt(ContinueStmt stmt) {
		return "continue;";
	}

	@Override
	public String visitBlockStmt(BlockStmt stmt) {
		if (stmt.statements.isEmpty()) {
			return "{}";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		indent++;
		for (Stmt statement : stmt.statements) {
			if (statement != null) {
				sb.append(indent()).append(statement.accept(this)).append("\n");
			}
		}
		indent--;
		sb.append(indent()).append("}");
		return sb.toString();
	}

	@Override
	public String visitExprStmt(ExprStmt stmt) {
		return stmt.expression.accept(this) + ";";
	}
}