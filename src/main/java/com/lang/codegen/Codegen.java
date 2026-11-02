package com.lang.codegen;

import com.lang.ast.*;
import com.lang.symbol.ClassSymbol;
import com.lang.symbol.FunSymbol;
import com.lang.symbol.MemberSymbol;
import com.lang.symbol.OperatorSymbol;
import com.lang.symbol.ParamSymbol;
import com.lang.symbol.Symbol;
import com.lang.symbol.SymbolTable;
import com.lang.symbol.VarSymbol;
import com.lang.unit.CompilationUnit;

import java.util.*;

public class Codegen {

    /* ===================== constantes ===================== */

    private static final Set<String> PRIMORDIAL_CLASSES = Set.of(
            "Any", "Void", "Null", "Stringfiable",
            "Bool", "Int", "Float", "String");

    private static final Map<String, String> STUB_FNS = Map.of(
            "print", "function_print",
            "println", "function_println",
            "isNull", "function_is_null");

    private static final Map<Character, String> OPNAME = Map.ofEntries(
            Map.entry('+', "plus"), Map.entry('-', "minus"),
            Map.entry('*', "star"), Map.entry('/', "slash"),
            Map.entry('%', "percent"), Map.entry('=', "equals"),
            Map.entry('!', "bang"), Map.entry('<', "langle"),
            Map.entry('>', "rangle"), Map.entry('&', "amp"),
            Map.entry('|', "bar"), Map.entry('?', "question"),
            Map.entry('~', "tilde"), Map.entry(':', "colon"));

    /* ===================== estado ===================== */

    private final CompilationUnit unit;
    private final SymbolTable symbols;
    private final Map<String, ClassInfo> classes = new LinkedHashMap<>();
    private final Map<String, TypeInstance> typeInstances = new LinkedHashMap<>();
    private final Set<String> symbolFunctionNames = new LinkedHashSet<>();

    private final Deque<Map<String, Binding>> scopes = new ArrayDeque<>();
    private final Set<String> currentTypeParams = new HashSet<>();
    private String currentClass = null;
    private boolean collected = false;

    private static final class Binding {
        Typed type;
        String cName;
    }

    private static final class ClassInfo {
        ClassDecl decl;
        String name;
        String parentName;
        ClassInfo parent;
        List<ParamDecl> ctorParams = List.of();
        List<VarDecl> fields = new ArrayList<>();
        List<FunDecl> methods = new ArrayList<>();
        List<OperatorDecl> operators = new ArrayList<>();
        List<TypeParamDecl> typeParams = new ArrayList<>();
        Map<String, FunDecl> fnByName = new LinkedHashMap<>();
        Map<String, OperatorDecl> opByKey = new LinkedHashMap<>();
        int classIndex;
    }

    private static final class TypeInstance {
        final ClassInfo generic;
        final List<Typed> args;
        final String signature;
        int classIndex;

        TypeInstance(ClassInfo g, List<Typed> args, String sig) {
            this.generic = g;
            this.args = args;
            this.signature = sig;
        }
    }

    private record VariableSlot(String name) {
    }

    public Codegen(CompilationUnit unit, SymbolTable symbols) {
        this.unit = unit;
        this.symbols = symbols;
    }

    /* ===================== API ===================== */

    public String header() {
        collect();
        String guard = headerGuard();
        StringBuilder sb = new StringBuilder();
        sb.append("/* auto generated */\n");
        sb.append("#ifndef ").append(guard).append("\n");
        sb.append("#define ").append(guard).append("\n\n");
        sb.append("#include \"runtime.h\"\n\n");

        for (ClassInfo ci : classes.values()) {
            if (isPrimordial(ci.name))
                continue;
            sb.append("typedef struct ").append(ci.name).append(" ").append(ci.name).append(";\n");
        }
        sb.append("\n");

        for (ClassInfo ci : topoOrder())
            if (!isPrimordial(ci.name))
                emitStructDef(ci, sb);
        for (ClassInfo ci : topoOrder())
            if (!isPrimordial(ci.name))
                emitClassDecls(ci, sb);
        emitTopLevelDecls(sb);
        sb.append("\n#endif /* ").append(guard).append(" */\n");
        return sb.toString();
    }

    public String source() {
        collect();
        StringBuilder sb = new StringBuilder();
        sb.append("/* auto generated */\n");
        sb.append("#include \"").append(baseName()).append(".h\"\n");
        sb.append("#include <stdlib.h>\n");
        sb.append("#include <string.h>\n");
        sb.append("#include <stdio.h>\n\n");

        for (ClassInfo ci : topoOrder())
            if (!isPrimordial(ci.name))
                emitClassImpl(ci, sb);
        emitTopLevelImpls(sb);
        emitGlobalOperatorExtension(sb);
        emitRuntimeInit(sb);
        return sb.toString();
    }

    /* ===================== nome do módulo ===================== */

    private String baseName() {
        String raw = unit.getFileName();
        if (raw == null || raw.isEmpty())
            raw = "lang_gen";
        int slash = Math.max(raw.lastIndexOf('/'), raw.lastIndexOf('\\'));
        if (slash >= 0)
            raw = raw.substring(slash + 1);
        int dot = raw.lastIndexOf('.');
        if (dot > 0)
            raw = raw.substring(0, dot);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_')
                sb.append(c);
            else
                sb.append('_');
        }
        String base = sb.toString();
        if (base.isEmpty())
            base = "lang_gen";
        if (Character.isDigit(base.charAt(0)))
            base = "_" + base;
        return base;
    }

    private String headerGuard() {
        return baseName().toUpperCase(Locale.ROOT) + "_H";
    }

    /* ===================== coleta ===================== */

    private void collect() {
        if (collected)
            return;
        collected = true;

        if (symbols != null) {
            for (FunSymbol function : symbols.getFuns().values()) {
                if (function.node instanceof FunDecl declaration
                        && !unit.getFuns().contains(declaration))
                    symbolFunctionNames.add(function.getName());
            }
            for (ClassSymbol symbol : symbols.getClasses().values()) {
                if (symbol.node instanceof ClassDecl declaration)
                    addClass(declaration);
            }
        }
        for (ClassDecl cd : unit.getClasses()) {
            addClass(cd);
        }
        for (ClassInfo ci : classes.values())
            ci.parent = classes.get(ci.parentName);

        collectTypeInstantiations();
        assignClassIndices();
    }

    private void addClass(ClassDecl cd) {
        ClassInfo ci = new ClassInfo();
        ci.decl = cd;
        ci.name = cd.name.source;
        if (cd.parameters != null)
            ci.ctorParams = cd.parameters;
        if (cd.typeParameters != null)
            ci.typeParams = cd.typeParameters;
        ClassSymbol symbol = cd.symbol instanceof ClassSymbol resolved
                ? resolved
                : symbols != null ? symbols.getClass(ci.name) : null;
        List<ClassSymbol> supers = symbol != null ? symbol.getBaseSupers() : List.of();
        ci.parentName = !supers.isEmpty() ? supers.get(0).getName() : "Any";

        if (cd.members != null)
            for (MemberDecl m : cd.members) {
                if (m instanceof VarDecl)
                    ci.fields.add((VarDecl) m);
                else if (m instanceof FunDecl) {
                    FunDecl f = (FunDecl) m;
                    ci.methods.add(f);
                    ci.fnByName.put(f.name.source, f);
                } else if (m instanceof OperatorDecl) {
                    OperatorDecl od = (OperatorDecl) m;
                    ci.operators.add(od);
                    ci.opByKey.put(opKey(od.kind, od.operator.lexeme), od);
                }
            }
        classes.put(ci.name, ci);
    }

    private void collectTypeInstantiations() {
        for (ClassInfo ci : classes.values()) {
            for (VarDecl v : ci.fields)
                visitType(v.type);
            for (FunDecl f : ci.methods) {
                visitType(f.type);
                if (f.parameters != null)
                    for (ParamDecl p : f.parameters)
                        visitType(p.type);
                visitStmts(f.body);
            }
            for (OperatorDecl od : ci.operators) {
                for (ParamDecl operand : od.operands)
                    visitType(operand.type);
                visitType(od.type);
                visitStmts(od.body);
            }
            for (ParamDecl p : ci.ctorParams)
                visitType(p.type);
        }
        for (VarDecl v : unit.getVars())
            visitType(v.type);
        for (FunDecl f : unit.getFuns()) {
            visitType(f.type);
            if (f.parameters != null)
                for (ParamDecl p : f.parameters)
                    visitType(p.type);
            visitStmts(f.body);
        }
        if (unit.getOperators() != null)
            for (OperatorDecl od : unit.getOperators()) {
                for (ParamDecl operand : od.operands)
                    visitType(operand.type);
                visitType(od.type);
                visitStmts(od.body);
            }
    }

    private void visitStmts(BlockStmt b) {
        if (b != null)
            for (Stmt s : b.statements)
                visitStmt(s);
    }

    private void visitStmt(Stmt s) {
        if (s == null)
            return;
        if (s instanceof LetStmt)
            visitType(((LetStmt) s).type);
        else if (s instanceof IfStmt) {
            IfStmt i = (IfStmt) s;
            visitStmt(i.thenBranch);
            visitStmt(i.elseBranch);
            visitExpr(i.condition);
        } else if (s instanceof WhileStmt) {
            WhileStmt w = (WhileStmt) s;
            visitExpr(w.condition);
            visitStmt(w.body);
        } else if (s instanceof BlockStmt)
            visitStmts((BlockStmt) s);
        else if (s instanceof ReturnStmt)
            visitExpr(((ReturnStmt) s).value);
        else if (s instanceof ExprStmt)
            visitExpr(((ExprStmt) s).expression);
    }

    private void visitExpr(Expr e) {
        if (e == null)
            return;
        if (e instanceof ParenthesisExpr)
            visitExpr(((ParenthesisExpr) e).inner);
        else if (e instanceof MemberExpr)
            visitExpr(((MemberExpr) e).object);
        else if (e instanceof CallExpr) {
            CallExpr c = (CallExpr) e;
            visitExpr(c.callee);
            if (c.arguments != null)
                for (Expr a : c.arguments)
                    visitExpr(a);
        } else if (e instanceof OperatorExpr) {
            OperatorExpr oe = (OperatorExpr) e;
            for (Expr operand : oe.operands)
                visitExpr(operand);
        }
    }

    private void visitType(Typed t) {
        if (t == null)
            return;
        if (t instanceof ParameterizedRefTyped) {
            ParameterizedRefTyped pt = (ParameterizedRefTyped) t;
            ClassInfo generic = classes.get(pt.getName());
            if (generic != null && !generic.typeParams.isEmpty()) {
                String sig = signatureOf(pt);
                if (!typeInstances.containsKey(sig)) {
                    typeInstances.put(sig, new TypeInstance(generic, pt.typeArguments, sig));
                }
            }
            for (Typed a : pt.typeArguments)
                visitType(a);
        }
    }

    private void assignClassIndices() {
        int idx = 20; // Inicia após os builtins do runtime
        for (ClassInfo ci : classes.values()) {
            if (isPrimordial(ci.name))
                continue;
            ci.classIndex = idx++;
        }
        for (TypeInstance inst : typeInstances.values()) {
            inst.classIndex = idx++;
        }
    }

    private static String signatureOf(ParameterizedRefTyped pt) {
        StringBuilder sb = new StringBuilder(pt.getName()).append('<');
        List<Typed> args = pt.typeArguments;
        for (int i = 0; i < args.size(); i++) {
            if (i > 0)
                sb.append(',');
            sb.append(args.get(i).getName());
        }
        return sb.append('>').toString();
    }

    /* ===================== topologia ===================== */

    private List<ClassInfo> topoOrder() {
        List<ClassInfo> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ClassInfo ci : classes.values())
            topoVisit(ci, out, seen);
        return out;
    }

    private void topoVisit(ClassInfo ci, List<ClassInfo> out, Set<String> seen) {
        if (!seen.add(ci.name))
            return;
        if (ci.parent != null)
            topoVisit(ci.parent, out, seen);
        out.add(ci);
    }

    private static boolean isPrimordial(String name) {
        return PRIMORDIAL_CLASSES.contains(name);
    }

    /* ===================== nomes ===================== */

    private static String kindName(int kind) {
        return kind == Operator.PREFIX ? "prefix"
                : kind == Operator.INFIX ? "infix"
                        : "postfix";
    }

    private static String opName(String lex) {
        if (lex == null || lex.isEmpty())
            return "op";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lex.length(); i++) {
            if (i > 0)
                sb.append('_');
            String m = OPNAME.get(lex.charAt(i));
            sb.append(m != null ? m : String.format("u%04x", (int) lex.charAt(i)));
        }
        return sb.toString();
    }

    private static String opKey(int kind, String lex) {
        return kindName(kind) + "|" + lex;
    }

    private static String typeSig(Typed t, String ctx) {
        if (t == null)
            return "Void";
        String n = t.getName();
        if ("Self".equals(n))
            return ctx != null ? ctx : "Any";
        if (n != null && n.length() == 1 && Character.isUpperCase(n.charAt(0)))
            return "generic_" + n;
        return (n == null || n.isEmpty()) ? "Void" : n;
    }

    private static String globalFunctionName(String name) {
        return "function_" + name;
    }

    private static String globalVarName(String name) {
        return "variable_" + name;
    }

    /* ===================== tipos C ===================== */

    private String ctype(Typed t) {
        return "Value*";
    }

    private String ctypeOrValue(Typed t) {
        return "Value*";
    }

    /* ===================== header ===================== */

    private void emitStructDef(ClassInfo ci, StringBuilder sb) {
        sb.append("struct ").append(ci.name).append(" {\n");
        sb.append("    const VTable* vtable;\n");
        sb.append("};\n\n");
    }

    private List<VariableSlot> variableSlots(ClassInfo ci) {
        List<VariableSlot> variables = new ArrayList<>();
        for (VarDecl field : ci.fields)
            variables.add(new VariableSlot(field.name.source));
        for (ParamDecl parameter : ci.ctorParams)
            variables.add(new VariableSlot(parameter.name.source));
        return variables;
    }

    private void emitClassDecls(ClassInfo ci, StringBuilder sb) {
        sb.append("Value* ").append(ci.name).append("_new(List* args);\n");
        sb.append("VTable* ").append(ci.name).append("__vt(void);\n");
        for (FunDecl function : ci.methods)
            sb.append("Value* ").append(ci.name).append("__")
                    .append(function.name.source)
                    .append("(const Value* self, List* args);\n");
        for (OperatorDecl operator : ci.operators)
            sb.append("Value* ").append(ci.name).append("__")
                    .append(globalOperatorCName(operator)).append("(List* args);\n");
        sb.append("\n");
    }

    private void emitTopLevelDecls(StringBuilder sb) {
        for (VarDecl v : unit.getVars())
            sb.append(ctypeOrValue(v.type)).append(" ").append(globalVarName(v.name.source)).append(";\n");

        Set<String> declaredFunctions = new HashSet<>();
        for (FunDecl f : unit.getFuns()) {
            sb.append("Value* ").append(globalFunctionName(f.name.source))
                    .append("(List* args);\n");
            declaredFunctions.add(f.name.source);
        }
        for (String name : symbolFunctionNames) {
            if (!STUB_FNS.containsKey(name) && declaredFunctions.add(name))
                sb.append("Value* ").append(globalFunctionName(name)).append("(List* args);\n");
        }
        for (OperatorDecl od : unit.getOperators()) {
            sb.append("Value* ").append(globalOperatorCName(od)).append("(List* args);\n");
        }
        sb.append("\n");
    }

    private String globalOperatorCName(OperatorDecl od) {
        StringBuilder sb = new StringBuilder("operator_")
                .append(kindName(od.kind)).append("_").append(opName(od.operator.lexeme));
        for (ParamDecl operand : od.operands)
            sb.append("_").append(typeSig(operand.type, null));
        return sb.toString();
    }

    private String globalOperatorParams(OperatorDecl od) {
        List<String> ps = new ArrayList<>();
        for (ParamDecl operand : od.operands)
            ps.add(ctype(operand.type) + " " + operand.name.source);
        return String.join(", ", ps);
    }

    /* ===================== source ===================== */

    private void emitOperatorImpl(ClassInfo ci, OperatorDecl od, StringBuilder sb) {
        String name = ci.name + "__" + globalOperatorCName(od);
        sb.append("Value* ").append(name).append("(List* args) {\n");

        pushScope();
        String previousClass = currentClass;
        currentClass = ci.name;

        for (int i = 0; i < od.operands.size(); i++) {
            ParamDecl operand = od.operands.get(i);
            bindTyped(operand.name.source, operand.type, "List_arg(args, " + i + ")");
        }

        if (od.body != null) {
            for (Stmt s : od.body.statements)
                stmt(s, sb, 1);
        }

        if (!alwaysReturns(od.body))
            sb.append("    return ").append(defaultReturn(od.type)).append(";\n");
        sb.append("}\n\n");

        currentClass = previousClass;
        popScope();
    }

    private void emitOperatorSlot(ClassInfo ci, OperatorDecl od, StringBuilder sb) {
        List<String> signatures = new ArrayList<>();
        List<String> runtimeTypes = new ArrayList<>();
        for (ParamDecl operand : od.operands) {
            signatures.add(typeSig(operand.type, ci.name));
            runtimeTypes.add(runtimeOperatorType(operand.type, ci));
        }
        String signature = String.join("_", signatures);
        int arity = od.operands.size();
        String firstType = runtimeTypes.get(0);
        String secondType = arity > 1 ? runtimeTypes.get(1) : null;

        String kind = kindName(od.kind);
        String op = opName(od.operator.lexeme);
        sb.append("VSLOT_OP_TYPED(SLOT_OP_NAME(\"").append(kind).append("\", \"")
                .append(op).append("\", \"").append(signature).append("\"), ")
                .append("SLOT_OP_KEY(\"").append(kind).append("\", \"")
                .append(op).append("\"), &").append(ci.name).append("__")
                .append(globalOperatorCName(od)).append(", ").append(arity)
                .append(", \"").append(firstType).append("\", ")
                .append(secondType == null ? "NULL" : "\"" + secondType + "\"")
                .append(")");
    }

    private String runtimeOperatorType(Typed type, ClassInfo owner) {
        return runtimeOperatorType(type, owner,
                owner != null ? owner.typeParams : List.of());
    }

    private String runtimeOperatorType(Typed type, ClassInfo owner,
            List<TypeParamDecl> typeParams) {
        if (type == null)
            return "Any";
        String name = type.getName();
        if ("Self".equals(name))
            return owner != null ? owner.name : "Any";
        if (typeParams != null)
            for (TypeParamDecl param : typeParams)
                if (param.name.sourceEquals(name))
                    return "Any";
        return name == null || name.isEmpty() ? "Any" : name;
    }

    private List<OperatorDecl> globalOperators() {
        return unit.getOperators();
    }

    private List<OperatorDecl> implementedGlobalOperators() {
        return globalOperators().stream().filter(operator -> operator.body != null).toList();
    }

    private void emitGlobalOperatorExtension(StringBuilder sb) {
        List<OperatorDecl> operators = globalOperators();
        if (operators.isEmpty())
            return;

        sb.append("static const VSlot global_operator_slots[] = {\n");
        for (OperatorDecl od : operators) {
            sb.append("    ");
            emitGlobalOperatorSlot(od, sb);
            sb.append(",\n");
        }
        sb.append("};\n\n");
        sb.append("static void* global_operator_slot_ptrs[] = {\n");
        for (int i = 0; i < operators.size(); i++)
            sb.append("    (void*)&global_operator_slots[").append(i).append("],\n");
        sb.append("};\n\n");
    }

    private void emitGlobalOperatorSlot(OperatorDecl od, StringBuilder sb) {
        List<String> signatures = new ArrayList<>();
        List<String> runtimeTypes = new ArrayList<>();
        for (ParamDecl operand : od.operands) {
            signatures.add(typeSig(operand.type, null));
            runtimeTypes.add(runtimeOperatorType(operand.type, null, od.typeParameters));
        }
        String signature = String.join("_", signatures);
        int arity = od.operands.size();
        String firstType = runtimeTypes.get(0);
        String secondType = arity > 1 ? runtimeTypes.get(1) : null;

        String kind = kindName(od.kind);
        String op = opName(od.operator.lexeme);
        sb.append("VSLOT_OP_TYPED(SLOT_OP_NAME(\"").append(kind).append("\", \"")
                .append(op).append("\", \"").append(signature).append("\"), ")
                .append("SLOT_OP_KEY(\"").append(kind).append("\", \"")
                .append(op).append("\"), &").append(globalOperatorCName(od))
                .append(", ").append(arity).append(", \"").append(firstType)
                .append("\", ")
                .append(secondType == null ? "NULL" : "\"" + secondType + "\"")
                .append(")");
    }

    private void emitClassImpl(ClassInfo ci, StringBuilder sb) {
        sb.append("/* ==== ").append(ci.name).append(" ==== */\n");

        for (FunDecl f : ci.methods) {
            if (f.body != null) {
                sb.append("Value* ").append(ci.name).append("__").append(f.name.source)
                        .append("(const Value* self, List* args) {\n");
                emitMethodBody(ci, f, sb);
                sb.append("}\n\n");
            }
        }

        for (OperatorDecl od : ci.operators) {
            if (od.body != null)
                emitOperatorImpl(ci, od, sb);
        }

        List<VariableSlot> variables = variableSlots(ci);

        // Slots array
        sb.append("static const VSlot ").append(ci.name).append("__slots[] = {\n");
        for (VariableSlot variable : variables) {
            sb.append("    VSLOT_VAR(SLOT_VAR_NAME(\"").append(variable.name()).append("\")),\n");
        }
        for (FunDecl f : ci.methods) {
            String slotName = "SLOT_FN_NAME(\"" + f.name.source + "\", \"void\")";
            sb.append("    VSLOT_FN(").append(slotName).append(", &").append(ci.name).append("__").append(f.name.source)
                    .append("),\n");
        }
        for (OperatorDecl od : ci.operators) {
            sb.append("    ");
            emitOperatorSlot(ci, od, sb);
            sb.append(",\n");
        }
        sb.append("};\n\n");

        sb.append("static void* ").append(ci.name).append("__slot_ptrs[] = {\n");

        int slotIndex = 0;
        for (; slotIndex < variables.size(); slotIndex++) {
            sb.append("    (void*)&").append(ci.name)
                    .append("__slots[").append(slotIndex).append("],\n");
        }

        for (int i = 0; i < ci.methods.size(); i++, slotIndex++) {
            sb.append("    (void*)&").append(ci.name)
                    .append("__slots[").append(slotIndex).append("],\n");
        }

        for (int i = 0; i < ci.operators.size(); i++) {
            sb.append("    (void*)&").append(ci.name)
                    .append("__slots[").append(slotIndex + i).append("],\n");
        }

        sb.append("};\n\n");

        // Info e Ponteiros de Super
        sb.append("VTableInfo ").append(ci.name).append("_info;\n");
        sb.append("\n");

        sb.append("DEF_BUILD(").append(ci.name).append(")\n");
        sb.append("DEF_VT(").append(ci.name).append(")\n\n");

        // Create & Destroy
        sb.append("static Value* ").append(ci.name).append("__create(List* args) {\n");
        sb.append("    (void)args;\n");
        sb.append("    ").append(ci.name).append("* obj = (").append(ci.name).append("*)calloc(1, sizeof(").append(ci.name)
                .append("));\n");
        sb.append("    if (!obj) return NULL;\n");
        sb.append("    obj->vtable = ").append(ci.name).append("__vt();\n");
        sb.append("    return Value_new(obj);\n");
        sb.append("}\n\n");

        sb.append("static Value* ").append(ci.name).append("__destroy(Value* self) {\n");
        sb.append("    if (!self || !self->ptr) return NULL;\n");
        sb.append("    free(self->ptr);\n");
        sb.append("    return NULL;\n");
        sb.append("}\n\n");

        // Construtor público _new
        sb.append("Value* ").append(ci.name).append("_new(List* args) {\n");

        pushScope();

        for (int i = 0; i < ci.ctorParams.size(); i++) {
            ParamDecl p = ci.ctorParams.get(i);
            sb.append("    Value* ").append(p.name.source)
                    .append(" = List_arg(args, ").append(i).append(");\n");
            bindTyped(p.name.source, p.type, p.name.source);
        }

        sb.append("    ").append(ci.name).append("* obj = (")
                .append(ci.name).append("*)calloc(1, sizeof(")
                .append(ci.name).append("));\n");

        sb.append("    if (!obj) return NULL;\n");
        sb.append("    obj->vtable = ").append(ci.name).append("__vt();\n");

        sb.append("    Value* instance = Value_new(obj);\n");
        sb.append("    if (!instance) { free(obj); return NULL; }\n");
        for (int i = 0; i < ci.ctorParams.size(); i++) {
            ParamDecl p = ci.ctorParams.get(i);
            sb.append("    rt_var_set(instance, \"").append(escapeString(p.name.source))
                    .append("\", ").append(p.name.source).append(");\n");
        }
        sb.append("    Value* self = instance;\n");
        sb.append("    (void)self;\n");

        String prev = currentClass;
        currentClass = ci.name;

        for (int i = 0; i < ci.fields.size(); i++) {
            VarDecl v = ci.fields.get(i);
            if (v.value != null)
                sb.append("    rt_var_set(instance, \"").append(escapeString(v.name.source))
                        .append("\", ").append(expr(v.value)).append(");\n");
        }

        currentClass = prev;
        popScope();

        sb.append("    return instance;\n");
        sb.append("}\n\n");
    }

    private void emitMethodBody(ClassInfo ci, FunDecl f, StringBuilder sb) {
        pushScope();
        String prev = currentClass;
        currentClass = ci.name;
        bind("self", ci.name, "(((" + ci.name + "*)(self->ptr)))");
        if (f.parameters != null) {
            for (int i = 0; i < f.parameters.size(); i++) {
                ParamDecl p = f.parameters.get(i);
                bindTyped(p.name.source, p.type, "List_arg(args, " + i + ")");
            }
        }
        if (f.body != null)
            for (Stmt s : f.body.statements)
                stmt(s, sb, 1);
        if (!alwaysReturns(f.body))
            sb.append("    return ").append(defaultReturn(f.type)).append(";\n");
        currentClass = prev;
        popScope();
    }

    private void emitRuntimeInit(StringBuilder sb) {
        sb.append("void ").append(baseName()).append("_init(void) {\n");
        sb.append("    runtime_init();\n");
        if (!globalOperators().isEmpty()) {
            sb.append("    if (!rt_register_slots(&__global_info, global_operator_slot_ptrs, ")
                    .append(globalOperators().size()).append(")) {\n")
                    .append("        fputs(\"runtime error: could not register global operators\\n\", stderr);\n")
                    .append("        abort();\n")
                    .append("    }\n");
        }
        for (ClassInfo ci : topoOrder()) {
            if (isPrimordial(ci.name))
                continue;
            sb.append("    DEF_TYPE_INIT(").append(ci.name).append(", ")
                    .append(variableSlots(ci).size() + ci.methods.size() + ci.operators.size()).append(", ")
                    .append(ci.name).append("__destroy, (void *)(intptr_t)")
                    .append(ci.parentName).append("_info.index);\n\n");
        }
        sb.append("}\n\n");
        sb.append("__attribute__((constructor))\n");
        sb.append("static void ").append(baseName()).append("__auto_init(void) {\n");
        sb.append("    ").append(baseName()).append("_init();\n");
        sb.append("}\n");
    }

    private void emitTopLevelImpls(StringBuilder sb) {
        for (VarDecl v : unit.getVars()) {
            pushScope();
            String init = v.value != null ? expr(v.value) : defaultFor(v.type);
            sb.append(ctypeOrValue(v.type)).append(" ").append(globalVarName(v.name.source))
                    .append(" = ").append(init).append(";\n\n");
            popScope();
        }

        for (FunDecl f : unit.getFuns()) {
            if (f.body == null)
                continue;
            sb.append("Value* ").append(globalFunctionName(f.name.source))
                    .append("(List* args) {\n");

            pushScope();
            String prev = currentClass;
            currentClass = null;

            if (f.parameters != null) {
                for (int i = 0; i < f.parameters.size(); i++) {
                    ParamDecl p = f.parameters.get(i);
                    bindTyped(p.name.source, p.type, "List_arg(args, " + i + ")");
                }
            }

            if (f.body != null)
                for (Stmt s : f.body.statements)
                    stmt(s, sb, 1);

            if (!alwaysReturns(f.body))
                sb.append("    return ").append(defaultReturn(f.type)).append(";\n");
            sb.append("}\n\n");

            currentClass = prev;
            popScope();
        }
        for (OperatorDecl od : implementedGlobalOperators()) {
            String name = globalOperatorCName(od);

            sb.append("Value* ").append(name).append("(List* args) {\n");

            pushScope();

            for (int i = 0; i < od.operands.size(); i++) {
                ParamDecl operand = od.operands.get(i);
                bindTyped(operand.name.source, operand.type, "List_arg(args, " + i + ")");
            }

            if (od.body != null) {
                for (Stmt s : od.body.statements)
                    stmt(s, sb, 1);
            }

            if (!alwaysReturns(od.body))
                sb.append("    return ").append(defaultReturn(od.type)).append(";\n");
            sb.append("}\n\n");

            popScope();
        }
    }

    /* ===================== statements ===================== */

    private void stmt(Stmt s, StringBuilder sb, int ind) {
        String p = "    ".repeat(ind);

        if (s instanceof LetStmt) {
            LetStmt l = (LetStmt) s;
            sb.append(p).append("Value* ").append(l.name.source).append(" = ")
                    .append(l.value != null ? expr(l.value) : defaultFor(l.type)).append(";\n");
            bindTyped(l.name.source, l.type, l.name.source);
        } else if (s instanceof IfStmt) {
            IfStmt i = (IfStmt) s;
            sb.append(p).append("if (value_as_bool(").append(expr(i.condition)).append(")) ");
            block(i.thenBranch, sb, ind);
            if (i.elseBranch != null) {
                sb.append(" else ");
                block(i.elseBranch, sb, ind);
            }
            sb.append("\n");
        } else if (s instanceof WhileStmt) {
            WhileStmt w = (WhileStmt) s;
            sb.append(p).append("while (value_as_bool(").append(expr(w.condition)).append(")) ");
            block(w.body, sb, ind);
            sb.append("\n");
        } else if (s instanceof BlockStmt) {
            block((BlockStmt) s, sb, ind);
            sb.append("\n");
        } else if (s instanceof ReturnStmt) {
            ReturnStmt r = (ReturnStmt) s;
            sb.append(p).append("return ").append(r.value != null ? expr(r.value) : "Void_new()").append(";\n");
        } else if (s instanceof BreakStmt) {
            sb.append(p).append("break;\n");
        } else if (s instanceof ContinueStmt) {
            sb.append(p).append("continue;\n");
        } else if (s instanceof ExprStmt) {
            sb.append(p).append("(void)(").append(expr(((ExprStmt) s).expression))
                    .append(");\n");
        }
    }

    private void block(BlockStmt b, StringBuilder sb, int ind) {
        sb.append("{\n");
        pushScope();
        if (b != null)
            for (Stmt s : b.statements)
                stmt(s, sb, ind + 1);
        popScope();
        sb.append("    ".repeat(ind)).append("}");
    }

    private boolean alwaysReturns(BlockStmt body) {
        if (body == null)
            return false;
        for (Stmt statement : body.statements) {
            if (alwaysReturns(statement))
                return true;
        }
        return false;
    }

    private boolean alwaysReturns(Stmt statement) {
        if (statement instanceof ReturnStmt)
            return true;
        if (statement instanceof BlockStmt block)
            return alwaysReturns(block);
        if (statement instanceof IfStmt conditional)
            return conditional.elseBranch != null
                    && alwaysReturns(conditional.thenBranch)
                    && alwaysReturns(conditional.elseBranch);
        return false;
    }

    /* ===================== expressões ===================== */

    private String expr(Expr e) {
        if (e == null)
            return "Void_new()";

        if (e instanceof LiteralExpr) {
            LiteralExpr l = (LiteralExpr) e;
            switch (l.type) {
                case LiteralExpr.NULL:
                    return "Null_new()";
                case LiteralExpr.BOOL:
                    return "Bool_new(" + ("true".equals(l.value) ? "true" : "false") + ")";
                case LiteralExpr.INT:
                    return "Int_new(" + l.value.replace("_", "") + "LL)";
                case LiteralExpr.FLOAT:
                    return "Float_new(" + l.value.replace("_", "") + ")";
                case LiteralExpr.STRING:
                    return "String_new(\"" + escapeString(l.value) + "\")";
            }
        }

        if (e instanceof RefExpr) {
            RefExpr reference = (RefExpr) e;
            String n = reference.name.source;
            Binding b = lookup(n);
            if (b != null)
                return b.cName;
            Symbol symbol = reference.symbol;
            if (symbol instanceof FunSymbol function)
                return symbolFunctionCName(function.getName());
            if (symbol instanceof VarSymbol variable) {
                String variableName = variable.getName();
                if (isInstanceVariable(symbol))
                    return "rt_var_get(self, \"" + escapeString(variableName) + "\")";
                return globalVarName(variableName);
            }
            if (symbol instanceof ParamSymbol parameter && isInstanceVariable(symbol))
                return "rt_var_get(self, \"" + escapeString(parameter.getName()) + "\")";
            if (symbol instanceof ClassSymbol type)
                return type.getName();
            return n;
        }

        if (e instanceof MemberExpr) {
            MemberExpr m = (MemberExpr) e;
            String obj = expr(m.object);
            String name = m.symbol instanceof MemberSymbol member
                    ? member.getName() : m.name.source;
            return "rt_var_get(value_require_nonnull(" + obj + ", \""
                    + escapeString(name) + "\"), \""
                    + escapeString(name) + "\")";
        }

        if (e instanceof ParenthesisExpr)
            return "(" + expr(((ParenthesisExpr) e).inner) + ")";

        if (e instanceof CallExpr)
            return genCall((CallExpr) e);
        if (e instanceof OperatorExpr)
            return genOperator((OperatorExpr) e);
        return "Void_new()";
    }

    private String genCall(CallExpr c) {
        List<Expr> callArguments = c.arguments != null ? c.arguments : List.of();
        StringBuilder args = new StringBuilder("({ ");
        List<String> argumentNames = new ArrayList<>();
        for (int i = 0; i < callArguments.size(); i++) {
            String argumentName = "_arg_" + i;
            argumentNames.add(argumentName);
            args.append("Value* ").append(argumentName).append(" = ")
                    .append(expr(callArguments.get(i))).append("; ");
        }
        args.append("List _args = ").append(listElements(argumentNames)).append("; ");

        String target;
        boolean stubFunction = false;

        if (c.symbol instanceof FunSymbol function
                && function.getDeclaringClass() == null) {
            String n = function.getName();
            if (STUB_FNS.containsKey(n)) {
                target = STUB_FNS.get(n);
                stubFunction = true;
            } else
                target = symbolFunctionCName(n);
        } else if (c.symbol instanceof ClassSymbol type) {
            target = type.getName() + "_new";
        } else {
            return "Void_new()";
        }

        args.append(target)
                .append(stubFunction ? "(List_arg(&_args, 0)); })" : "(&_args); })");
        return args.toString();
    }

    private String symbolFunctionCName(String name) {
        return STUB_FNS.getOrDefault(name, globalFunctionName(name));
    }

    private String genOperator(OperatorExpr e) {
        OperatorSymbol resolved = e.symbol instanceof OperatorSymbol operator
                ? operator : null;
        int operatorKind = resolved != null ? resolved.getKind() : e.kind;
        String operatorLexeme = resolved != null
                ? resolved.getLexeme() : e.operator.lexeme;
        String kind = kindName(operatorKind);
        String name = opName(operatorLexeme);
        String operatorKey = "SLOT_OP_KEY(\"" + kind + "\", \"" + name + "\")";
        if (operatorKind == Operator.INFIX && "=".equals(operatorLexeme))
            return genAssignment(e, operatorKey);

        StringBuilder sb = new StringBuilder("({ ");
        List<String> argumentNames = new ArrayList<>();

        for (int i = 0; i < e.operands.size(); i++) {
            String argumentName = "_arg_" + i;
            sb.append("Value* ").append(argumentName).append(" = ")
                    .append(expr(e.operands.get(i))).append("; ");
            argumentNames.add(argumentName);
        }
        sb.append("List _args = ").append(listElements(argumentNames)).append("; ");

        String staticType = resolved != null && resolved.getDeclaringClass() != null
                ? resolved.getDeclaringClass().getName() + "__vt()" : "NULL";
        sb.append("Value* _self = List_arg(&_args, 0); ");
        sb.append("SlotOp _op = rt_find_operator_for_type(_self, ")
                .append(operatorKey).append(", &_args, ").append(staticType).append("); ");
        sb.append("_op ? _op(&_args) : rt_missing_operator(")
                .append(operatorKey).append("); })");

        return sb.toString();
    }

    private String genAssignment(OperatorExpr e, String operatorKey) {
        Expr targetExpr = e.operands.get(0);
        while (targetExpr instanceof ParenthesisExpr)
            targetExpr = ((ParenthesisExpr) targetExpr).inner;
        if (targetExpr instanceof MemberExpr member) {
            return genVariableAssignment(expr(member.object), member.name.source,
                    e.operands.get(1), operatorKey);
        }
        if (targetExpr instanceof RefExpr reference
                && lookup(reference.name.source) == null
                && isInstanceVariable(reference.symbol)) {
            return genVariableAssignment("(Value*)self", reference.name.source,
                    e.operands.get(1), operatorKey);
        }
        if (!(targetExpr instanceof RefExpr) && !(targetExpr instanceof MemberExpr))
            throw new IllegalArgumentException("Assignment target must be a variable or member");

        String target = expr(targetExpr);
        StringBuilder sb = new StringBuilder("({ Value** _target = &(")
                .append(target).append("); Value* _left = *_target; ")
                .append("Value* _right = ").append(expr(e.operands.get(1)))
                .append("; List _args = List_elements((void *[]){_left, _right}, 2); ")
                .append("SlotOp _op = rt_find_operator_for(_left, ")
                .append(operatorKey).append(", &_args); ")
                .append("Value* _result = _op ? _op(&_args) : rt_missing_operator(")
                .append(operatorKey).append("); ")
                .append("*_target = value_assign(_left, _result); *_target; })");
        return sb.toString();
    }

    private String genVariableAssignment(String object, String name, Expr value, String operatorKey) {
        String escapedName = escapeString(name);
        return "({ Value* _object = value_require_nonnull(" + object + ", \"" + escapedName + "\"); "
                + "Value* _left = rt_var_get(_object, \"" + escapedName + "\"); "
                + "Value* _right = " + expr(value) + "; "
                + "List _args = List_elements((void *[]){_left, _right}, 2); "
                + "SlotOp _op = rt_find_operator_for(_left, " + operatorKey + ", &_args); "
                + "Value* _result = _op ? _op(&_args) : rt_missing_operator(" + operatorKey + "); "
                + "rt_var_assign(_object, \"" + escapedName + "\", _result); })";
    }

    private String listElements(List<String> elements) {
        if (elements.isEmpty())
            return "List_elements(NULL, 0)";
        return "List_elements((void *[]){" + String.join(", ", elements) + "}, "
                + elements.size() + ")";
    }

    private boolean isInstanceVariable(Symbol symbol) {
        if (symbol instanceof VarSymbol variable)
            return variable.getDeclaringClass() != null;
        if (!(symbol instanceof ParamSymbol))
            return false;
        ClassInfo current = classes.get(currentClass);
        if (current == null || !(current.decl.symbol instanceof ClassSymbol classSymbol))
            return false;
        return classSymbol.getParameters().values().stream().anyMatch(parameter -> parameter == symbol);
    }

    private String defaultFor(Typed t) {
        if (t == null)
            return "Null_new()";
        switch (t.getName()) {
            case "Int":
                return "Int_new(0LL)";
            case "Float":
                return "Float_new(0.0)";
            case "Bool":
                return "Bool_new(false)";
            case "String":
                return "String_new(\"\")";
            case "Null":
                return "Null_new()";
            case "Void":
                return "Void_new()";
            default:
                return "Null_new()";
        }
    }

    private String defaultReturn(Typed t) {
        String tName = t != null ? t.getName() : "Void";
        if (t == null || "Void".equals(tName))
            return "Void_new()";
        return defaultFor(t);
    }

    private String args(List<Expr> list) {
        if (list == null || list.isEmpty())
            return "";
        List<String> out = new ArrayList<>();
        for (Expr e : list)
            out.add(expr(e));
        return String.join(", ", out);
    }

    private static String escapeString(String s) {
        if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"')
            s = s.substring(1, s.length() - 1);
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private void pushScope() {
        scopes.push(new HashMap<>());
    }

    private void popScope() {
        scopes.pop();
    }

    private void bindTyped(String name, Typed type, String cName) {
        if (scopes.isEmpty())
            return;
        Binding b = new Binding();
        b.type = type;
        b.cName = cName;
        scopes.peek().put(name, b);
    }

    private void bind(String name, String typeName, String cName) {
        bindTyped(name,
                typeName != null ? new RefTyped(new Identifier(typeName, null), null) : null,
                cName);
    }

    private Binding lookup(String n) {
        for (Map<String, Binding> s : scopes)
            if (s.containsKey(n))
                return s.get(n);
        return null;
    }
}
