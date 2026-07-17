package com.lang.tools;

import com.lang.ast.*;
import com.lang.util.Position;
import java.util.List;

public class Dumper implements ExprVisitor<String>, StmtVisitor<String> {
	private int indent = 0;
	private boolean showPositions = false;
	private boolean showTypes = true;

	public Dumper() {
		this(false, true);
	}

	public Dumper(boolean showPositions, boolean showTypes) {
		this.showPositions = showPositions;
		this.showTypes = showTypes;
	}

	public String dump(Node node) {
		indent = 0;
		if (node instanceof Expr) {
			return ((Expr) node).accept(this);
		} else if (node instanceof Stmt) {
			return ((Stmt) node).accept(this);
		}
		return "";
	}

	public String dump(List<? extends Node> nodes) {
		StringBuilder sb = new StringBuilder();
		for (Node node : nodes) {
			sb.append(dump(node));
		}
		return sb.toString();
	}

	private String indent() {
		if (indent == 0) return "";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < indent; i++) {
			sb.append("  ");
		}
		return sb.toString();
	}

	private String pos(Position p) {
		if (!showPositions || p == null) return "";
		return " [line=" + p.getLine() + ", start=" + p.getStart() + ", end=" + p.getEnd() + "]";
	}

	private String type(Class<?> clazz) {
		if (!showTypes) return "";
		String name = clazz.getSimpleName();
		return " <" + name + ">";
	}

	private String type(Expr expr) {
		if (!showTypes || expr == null) return "";
		return type(expr.getClass());
	}

	private String type(Stmt stmt) {
		if (!showTypes || stmt == null) return "";
		return type(stmt.getClass());
	}

	@Override
	public String visitLiteralExpr(LiteralExpr expr) {
		String value;
		if (expr.value == null || expr.isNull()) {
			value = "null";
		} else if (expr.isString()) {
			value = "\"" + expr.value.toString()
					.replace("\\", "\\\\")
					.replace("\"", "\\\"")
					.replace("\n", "\\n")
					.replace("\t", "\\t") + "\"";
		} else if (expr.isChar()) {
			value = "'" + expr.value.toString()
					.replace("\\", "\\\\")
					.replace("'", "\\'") + "'";
		} else {
			value = expr.value.toString();
		}
		return indent() + "Literal" + type(expr) + "(" + expr.getTypeName() + ": " + value + ")" + pos(expr.getPosition()) + "\n";
	}

	@Override
	public String visitRefExpr(RefExpr expr) {
		return indent() + "Ref" + type(expr) + "(" + expr.name + ")" + pos(expr.getPosition()) + "\n";
	}

	@Override
	public String visitAssignExpr(AssignExpr expr) {
		StringBuilder sb = new StringBuilder();
		sb.append(indent()).append("Assign").append(type(expr)).append(" {\n");
		indent++;
		sb.append(indent()).append("target: ").append(expr.target.accept(this));
		sb.append(indent()).append("operator: ").append(expr.operator.lexeme).append("\n");
		sb.append(indent()).append("value: ").append(expr.value.accept(this));
		indent--;
		sb.append(indent()).append("}").append(pos(expr.getPosition())).append("\n");
		return sb.toString();
	}

	@Override
	public String visitBinaryExpr(BinaryExpr expr) {
		StringBuilder sb = new StringBuilder();
		sb.append(indent()).append("Binary").append(type(expr)).append(" {\n");
		indent++;
		sb.append(indent()).append("left: ").append(expr.left.accept(this));
		sb.append(indent()).append("operator: ").append(expr.operator.lexeme).append("\n");
		sb.append(indent()).append("right: ").append(expr.right.accept(this));
		indent--;
		sb.append(indent()).append("}").append(pos(expr.getPosition())).append("\n");
		return sb.toString();
	}

	@Override
	public String visitUnaryExpr(UnaryExpr expr) {
		StringBuilder sb = new StringBuilder();
		sb.append(indent()).append("Unary").append(type(expr)).append(" {\n");
		indent++;
		sb.append(indent()).append("operator: ").append(expr.operator.lexeme).append("\n");
		sb.append(indent()).append("operand: ").append(expr.operand.accept(this));
		indent--;
		sb.append(indent()).append("}").append(pos(expr.getPosition())).append("\n");
		return sb.toString();
	}

	@Override
	public String visitTernaryExpr(TernaryExpr expr) {
		StringBuilder sb = new StringBuilder();
		sb.append(indent()).append("Ternary").append(type(expr)).append(" {\n");
		indent++;
		sb.append(indent()).append("condition: ").append(expr.condition.accept(this));
		sb.append(indent()).append("then: ").append(expr.thenExpr.accept(this));
		sb.append(indent()).append("else: ").append(expr.elseExpr.accept(this));
		indent--;
		sb.append(indent()).append("}").append(pos(expr.getPosition())).append("\n");
		return sb.toString();
	}

	@Override
	public String visitCallExpr(CallExpr expr) {
		StringBuilder sb = new StringBuilder();
		sb.append(indent()).append("Call").append(type(expr)).append(" {\n");
		indent++;
		sb.append(indent()).append("callee: ").append(expr.callee.accept(this));

		if (!expr.typeArguments.isEmpty()) {
			sb.append(indent()).append("typeArguments: [");
			for (int i = 0; i < expr.typeArguments.size(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(expr.typeArguments.get(i).getName());
			}
			sb.append("]\n");
		}

		sb.append(indent()).append("arguments: [\n");
		indent++;
		for (Expr arg : expr.arguments) {
			sb.append(arg.accept(this));
		}
		indent--;
		sb.append(indent()).append("]\n");
		indent--;
		sb.append(indent()).append("}").append(pos(expr.getPosition())).append("\n");
		return sb.toString();
	}

	@Override
	public String visitIndexAccessExpr(IndexAccessExpr expr) {
		StringBuilder sb = new StringBuilder();
		sb.append(indent()).append("IndexAccess").append(type(expr)).append(" {\n");
		indent++;
		sb.append(indent()).append("target: ").append(expr.target.accept(this));
		sb.append(indent()).append("index: ").append(expr.index.accept(this));
		indent--;
		sb.append(indent()).append("}").append(pos(expr.getPosition())).append("\n");
		return sb.toString();
	}

	@Override
	public String visitMemberAccessExpr(MemberAccessExpr expr) {
		StringBuilder sb = new StringBuilder();
		sb.append(indent()).append("MemberAccess").append(type(expr)).append(" {\n");
		indent++;
		sb.append(indent()).append("object: ").append(expr.object.accept(this));
		sb.append(indent()).append("name: ").append(expr.name).append("\n");
		indent--;
		sb.append(indent()).append("}").append(pos(expr.getPosition())).append("\n");
		return sb.toString();
	}

	// Statements

	@Override
	public String visitVarDeclStmt(VarDeclStmt stmt) {
		StringBuilder sb = new StringBuilder();
		sb.append(indent()).append("VarDecl").append(type(stmt)).append(" {\n");
		indent++;
		sb.append(indent()).append("name: ").append(stmt.name).append("\n");
		if (stmt.hasType()) {
			sb.append(indent()).append("type: ").append(stmt.type.getName()).append("\n");
		}
		if (stmt.hasValue()) {
			sb.append(indent()).append("value: ").append(stmt.value.accept(this));
		}
		indent--;
		sb.append(indent()).append("}").append(pos(stmt.getPosition())).append("\n");
		return sb.toString();
	}

	@Override
	public String visitFunDeclStmt(FunDeclStmt stmt) {
		StringBuilder sb = new StringBuilder();
		sb.append(indent()).append("FunDecl").append(type(stmt)).append(" {\n");
		indent++;
		sb.append(indent()).append("name: ").append(stmt.name).append("\n");

		if (stmt.hasTypeParameters()) {
			sb.append(indent()).append("typeParameters: [\n");
			indent++;
			for (TypeParamDeclStmt tp : stmt.typeParameters) {
				sb.append(tp.accept(this));
			}
			indent--;
			sb.append(indent()).append("]\n");
		}

		sb.append(indent()).append("parameters: [\n");
		indent++;
		for (ParamDeclStmt param : stmt.parameters) {
			sb.append(param.accept(this));
		}
		indent--;
		sb.append(indent()).append("]\n");

		if (stmt.hasReturnType()) {
			sb.append(indent()).append("returnType: ").append(stmt.returnType.getName()).append("\n");
		}

		if (stmt.hasBody()) {
			sb.append(indent()).append("body: ").append(stmt.body.accept(this));
		} else {
			sb.append(indent()).append("body: <abstract>\n");
		}
		indent--;
		sb.append(indent()).append("}").append(pos(stmt.getPosition())).append("\n");
		return sb.toString();
	}

	@Override
	public String visitParamDeclStmt(ParamDeclStmt stmt) {
		StringBuilder sb = new StringBuilder();
		sb.append(indent()).append("Param").append(type(stmt)).append(" {\n");
		indent++;
		sb.append(indent()).append("name: ").append(stmt.name).append("\n");
		if (stmt.hasType()) {
			sb.append(indent()).append("type: ").append(stmt.type.getName()).append("\n");
		}
		indent--;
		sb.append(indent()).append("}").append(pos(stmt.getPosition())).append("\n");
		return sb.toString();
	}

	@Override
	public String visitTypeParamDeclStmt(TypeParamDeclStmt stmt) {
		StringBuilder sb = new StringBuilder();
		sb.append(indent()).append("TypeParam").append(type(stmt)).append(" {\n");
		indent++;
		sb.append(indent()).append("name: ").append(stmt.name).append("\n");
		if (stmt.superclass != null) {
			sb.append(indent()).append("superclass: ").append(stmt.superclass.getName()).append("\n");
		}
		if (!stmt.supertraits.isEmpty()) {
			sb.append(indent()).append("supertraits: [");
			for (int i = 0; i < stmt.supertraits.size(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(stmt.supertraits.get(i).getName());
			}
			sb.append("]\n");
		}
		indent--;
		sb.append(indent()).append("}").append(pos(stmt.getPosition())).append("\n");
		return sb.toString();
	}

	@Override
	public String visitClassDeclStmt(ClassDeclStmt stmt) {
		StringBuilder sb = new StringBuilder();
		sb.append(indent()).append("Class").append(type(stmt)).append(" {\n");
		indent++;
		sb.append(indent()).append("name: ").append(stmt.name).append("\n");

		if (stmt.hasTypeParameters()) {
			sb.append(indent()).append("typeParameters: [\n");
			indent++;
			for (TypeParamDeclStmt tp : stmt.typeParameters) {
				sb.append(tp.accept(this));
			}
			indent--;
			sb.append(indent()).append("]\n");
		}

		if (stmt.hasSuperclass()) {
			sb.append(indent()).append("superclass: ").append(stmt.superclass.getName()).append("\n");
		}

		if (stmt.hasSupertraits()) {
			sb.append(indent()).append("supertraits: [");
			for (int i = 0; i < stmt.supertraits.size(); i++) {
				if (i > 0) sb.append(", ");
				sb.append(stmt.supertraits.get(i).getName());
			}
			sb.append("]\n");
		}

		if (!stmt.vars.isEmpty()) {
			sb.append(indent()).append("vars: [\n");
			indent++;
			for (VarDeclStmt var : stmt.vars) {
				sb.append(var.accept(this));
			}
			indent--;
			sb.append(indent()).append("]\n");
		}

		if (!stmt.funs.isEmpty()) {
			sb.append(indent()).append("funs: [\n");
			indent++;
			for (FunDeclStmt fun : stmt.funs) {
				sb.append(fun.accept(this));
			}
			indent--;
			sb.append(indent()).append("]\n");
		}

		indent--;
		sb.append(indent()).append("}").append(pos(stmt.getPosition())).append("\n");
		return sb.toString();
	}

	@Override
	public String visitLetDeclStmt(LetDeclStmt stmt) {
		StringBuilder sb = new StringBuilder();
		sb.append(indent()).append("LetDecl").append(type(stmt)).append(" {\n");
		indent++;
		sb.append(indent()).append("name: ").append(stmt.name).append("\n");
		if (stmt.hasType()) {
			sb.append(indent()).append("type: ").append(stmt.type.getName()).append("\n");
		}
		if (stmt.hasValue()) {
			sb.append(indent()).append("value: ").append(stmt.value.accept(this));
		}
		indent--;
		sb.append(indent()).append("}").append(pos(stmt.getPosition())).append("\n");
		return sb.toString();
	}

	@Override
	public String visitIfStmt(IfStmt stmt) {
		StringBuilder sb = new StringBuilder();
		sb.append(indent()).append("If").append(type(stmt)).append(" {\n");
		indent++;
		sb.append(indent()).append("condition: ").append(stmt.condition.accept(this));
		sb.append(indent()).append("then: ").append(stmt.thenBranch.accept(this));
		if (stmt.elseBranch != null) {
			sb.append(indent()).append("else: ").append(stmt.elseBranch.accept(this));
		}
		indent--;
		sb.append(indent()).append("}").append(pos(stmt.getPosition())).append("\n");
		return sb.toString();
	}

	@Override
	public String visitWhileStmt(WhileStmt stmt) {
		StringBuilder sb = new StringBuilder();
		sb.append(indent()).append("While").append(type(stmt)).append(" {\n");
		indent++;
		sb.append(indent()).append("condition: ").append(stmt.condition.accept(this));
		sb.append(indent()).append("body: ").append(stmt.body.accept(this));
		indent--;
		sb.append(indent()).append("}").append(pos(stmt.getPosition())).append("\n");
		return sb.toString();
	}

	@Override
	public String visitReturnStmt(ReturnStmt stmt) {
		StringBuilder sb = new StringBuilder();
		sb.append(indent()).append("Return").append(type(stmt)).append(" {\n");
		indent++;
		if (stmt.value != null) {
			sb.append(indent()).append("value: ").append(stmt.value.accept(this));
		} else {
			sb.append(indent()).append("value: <void>\n");
		}
		indent--;
		sb.append(indent()).append("}").append(pos(stmt.getPosition())).append("\n");
		return sb.toString();
	}

	@Override
	public String visitBreakStmt(BreakStmt stmt) {
		return indent() + "Break" + type(stmt) + pos(stmt.getPosition()) + "\n";
	}

	@Override
	public String visitContinueStmt(ContinueStmt stmt) {
		return indent() + "Continue" + type(stmt) + pos(stmt.getPosition()) + "\n";
	}

	@Override
	public String visitBlockStmt(BlockStmt stmt) {
		StringBuilder sb = new StringBuilder();
		sb.append(indent()).append("Block").append(type(stmt)).append(" {\n");
		indent++;
		for (Stmt s : stmt.statements) {
			sb.append(s.accept(this));
		}
		indent--;
		sb.append(indent()).append("}").append(pos(stmt.getPosition())).append("\n");
		return sb.toString();
	}

	@Override
	public String visitExprStmt(ExprStmt stmt) {
		StringBuilder sb = new StringBuilder();
		sb.append(indent()).append("ExprStmt").append(type(stmt)).append(" {\n");
		indent++;
		sb.append(indent()).append("expression: ").append(stmt.expression.accept(this));
		indent--;
		sb.append(indent()).append("}").append(pos(stmt.getPosition())).append("\n");
		return sb.toString();
	}
}