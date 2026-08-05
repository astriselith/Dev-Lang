package com.lang.tools;

import com.lang.ast.*;
import java.util.List;

public class Printer implements DeclVisitor<String>, StmtVisitor<String>, ExprVisitor<String> {
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
		return expr.name.source;
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

		if (expr.typeArguments != null && !expr.typeArguments.isEmpty()) {
			sb.append("<");
			for (int i = 0; i < expr.typeArguments.size(); i++) {
				if (i > 0)
					sb.append(", ");
				sb.append(expr.typeArguments.get(i).getSignature());
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
	public String visitVarDecl(VarDecl decl) {
		StringBuilder sb = new StringBuilder();

		sb.append("var ").append(decl.name);
		if (decl.type != null) {
			sb.append(": ").append(decl.type.getSignature());
		}
		if (decl.value != null) {
			sb.append(" = ").append(decl.value.accept(this));
		}
		sb.append(";");
		return sb.toString();
	}

	@Override
	public String visitFunDecl(FunDecl decl) {
		StringBuilder sb = new StringBuilder();

		sb.append("fun ").append(decl.name);

		if (decl.typeParameters != null) {
			sb.append("<");
			for (int i = 0; i < decl.typeParameters.size(); i++) {
				if (i > 0)
					sb.append(", ");
				sb.append(decl.typeParameters.get(i).accept(this));
			}
			sb.append(">");
		}

		sb.append("(");
		for (int i = 0; i < decl.parameters.size(); i++) {
			if (i > 0)
				sb.append(", ");
			ParamDecl param = decl.parameters.get(i);
			sb.append(param.accept(this));
		}
		sb.append(")");

		if (decl.returnType != null) {
			sb.append(": ").append(decl.returnType.getSignature());
		}

		if (decl.body != null) {
			sb.append(" ");
			sb.append(decl.body.accept(this));
		} else {
			sb.append(";");
		}
		return sb.toString();
	}

	@Override
	public String visitParamDecl(ParamDecl decl) {
		if (decl.type != null) {
			return decl.name + ": " + decl.type.getName();
		}
		return decl.name.source;
	}

	@Override
	public String visitTypeParamDecl(TypeParamDecl decl) {
		StringBuilder sb = new StringBuilder(decl.name.source);

		if (decl.supers != null && !decl.supers.isEmpty()) {
			sb.append(" : ");
			sb.append(String.join(" & ", decl.supers.stream()
					.map(s -> s.accept(this))
					.toArray(String[]::new)));
		}

		return sb.toString();
	}

	@Override
	public String visitClassDecl(ClassDecl decl) {
		StringBuilder sb = new StringBuilder();

		sb.append("class ").append(decl.name);

		if (decl.typeParameters != null) {
			sb.append("<");
			sb.append(String.join(", ", decl.typeParameters.stream()
					.map(p -> p.accept(this))
					.toArray(String[]::new)));
			sb.append(">");
		}

		if (decl.supers != null && !decl.supers.isEmpty()) {
			sb.append(" : ");

			sb.append(String.join(" & ", decl.supers.stream()
					.map(s -> s.accept(this))
					.toArray(String[]::new)));
		}

		sb.append(" ");

		if (decl.members.isEmpty()) {
			sb.append("{}");
		} else {
			sb.append("{\n");
			indent++;

			for (MemberDecl member : decl.members) {
				if (member != null) {
					sb.append(indent()).append(member.accept(this)).append("\n");
				}
			}

			indent--;
			sb.append(indent()).append("}");
		}
		return sb.toString();
	}

	@Override
	public String visitSuperDecl(SuperDecl decl) {
		return decl.type.getSignature();
	}

	@Override
	public String visitLetStmt(LetStmt stmt) {
		StringBuilder sb = new StringBuilder();
		sb.append("let ").append(stmt.name);
		if (stmt.type != null) {
			sb.append(": ").append(stmt.type.getSignature());
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