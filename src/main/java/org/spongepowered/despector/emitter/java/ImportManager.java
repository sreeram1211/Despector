/*
 * The MIT License (MIT)
 *
 * Copyright (c) Despector <https://despector.voxelgenesis.com>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.despector.emitter.java;

import com.google.common.collect.Lists;
import org.spongepowered.despector.ast.Annotation;
import org.spongepowered.despector.ast.Annotation.EnumConstant;
import org.spongepowered.despector.ast.Locals.LocalInstance;
import org.spongepowered.despector.ast.generic.ClassTypeSignature;
import org.spongepowered.despector.ast.generic.GenericClassTypeSignature;
import org.spongepowered.despector.ast.generic.TypeArgument;
import org.spongepowered.despector.ast.generic.TypeParameter;
import org.spongepowered.despector.ast.generic.TypeSignature;
import org.spongepowered.despector.ast.insn.InstructionVisitor;
import org.spongepowered.despector.ast.insn.cst.DoubleConstant;
import org.spongepowered.despector.ast.insn.cst.FloatConstant;
import org.spongepowered.despector.ast.insn.cst.IntConstant;
import org.spongepowered.despector.ast.insn.cst.LongConstant;
import org.spongepowered.despector.ast.insn.cst.NullConstant;
import org.spongepowered.despector.ast.insn.cst.StringConstant;
import org.spongepowered.despector.ast.insn.cst.TypeConstant;
import org.spongepowered.despector.ast.insn.misc.Cast;
import org.spongepowered.despector.ast.insn.misc.InstanceOf;
import org.spongepowered.despector.ast.insn.misc.MultiNewArray;
import org.spongepowered.despector.ast.insn.misc.NewArray;
import org.spongepowered.despector.ast.insn.misc.NumberCompare;
import org.spongepowered.despector.ast.insn.misc.Ternary;
import org.spongepowered.despector.ast.insn.op.NegativeOperator;
import org.spongepowered.despector.ast.insn.op.Operator;
import org.spongepowered.despector.ast.insn.var.ArrayAccess;
import org.spongepowered.despector.ast.insn.var.InstanceFieldAccess;
import org.spongepowered.despector.ast.insn.var.LocalAccess;
import org.spongepowered.despector.ast.insn.var.StaticFieldAccess;
import org.spongepowered.despector.ast.stmt.invoke.DynamicInvoke;
import org.spongepowered.despector.ast.stmt.invoke.InstanceMethodInvoke;
import org.spongepowered.despector.ast.stmt.invoke.New;
import org.spongepowered.despector.ast.stmt.invoke.StaticMethodInvoke;
import org.spongepowered.despector.ast.type.FieldEntry;
import org.spongepowered.despector.ast.type.MethodEntry;
import org.spongepowered.despector.ast.type.TypeEntry;
import org.spongepowered.despector.ast.type.TypeEntry.InnerClassInfo;
import org.spongepowered.despector.util.TypeHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A manager which handles determining which types should be imported and
 * whether a given type is already imported.
 */
public class ImportManager {

    private final List<String> implicit_imports = new ArrayList<>();
    private final Set<String> imports = new HashSet<>();
    private final Set<TypeEntry> checked = new HashSet<>();

    public ImportManager() {
        addImplicitImport("java/lang/");
    }

    /**
     * Clears all imports from this manager.
     */
    public void reset() {
        this.imports.clear();
        this.checked.clear();
    }

    /**
     * Adds the given package prefix as an implicit import. Implicit imports are
     * not emitted at the top of the class but are still considered present.
     * 
     * <p>An example of an implicit import is {@code 'java/lang/'} which is
     * always considered as imported in java code.</p>
     */
    public void addImplicitImport(String i) {
        if (!this.implicit_imports.contains(i)) {
            this.implicit_imports.add(i);
        }
    }

    /**
     * Traverses the given type and determines what types should be imported for
     * it.
     */
    public void calculateImports(TypeEntry type) {
        if (this.checked.contains(type)) {
            return;
        }
        this.checked.add(type);
        ImportWalker walker = new ImportWalker();

        for (Annotation anno : type.getAnnotations()) {
            check(anno);
        }
        for (String i : type.getInterfaces()) {
            add("L" + i + ";");
        }
        for (MethodEntry method : type.getStaticMethods()) {
            check(method, walker);
        }
        for (MethodEntry method : type.getMethods()) {
            check(method, walker);
        }
        for (FieldEntry field : type.getStaticFields()) {
            check(field);
        }
        for (FieldEntry field : type.getFields()) {
            check(field);
        }
        for (InnerClassInfo info : type.getInnerClasses()) {
            TypeEntry inner = type.getSource().get(info.getName());
            if (inner != null && inner != type) {
                calculateImports(inner);
            }
        }
    }

    /**
     * Adds the given type descriptor to the list of imports.
     */
    void add(String desc) {
        if (desc.startsWith("[")) {
            add(desc.substring(1));
            return;
        }
        if (!desc.startsWith("L")) {
            return;
        }
        String type = TypeHelper.descToType(desc);
        if (type.indexOf('$') != -1) {
            type = type.substring(0, type.indexOf('$'));
        }
        for (String implicit : this.implicit_imports) {
            if (type.startsWith(implicit) && type.lastIndexOf('/') <= implicit.length()) {
                return;
            }
        }
        this.imports.add(type);
    }

    private void check(Annotation anno) {
        add(anno.getType().getName());
        for (String key : anno.getKeys()) {
            Object val = anno.getValue(key);
            checkAnnotationValue(val);
        }
    }

    private void checkAnnotationValue(Object val) {
        if (val instanceof ClassTypeSignature) {
            add(((ClassTypeSignature) val).getDescriptor());
        } else if (val instanceof GenericClassTypeSignature) {
            add(((ClassTypeSignature) val).getDescriptor());
        } else if (val instanceof EnumConstant) {
            add(((EnumConstant) val).getEnumType());
        } else if (val instanceof List) {
            for (Object obj : (List<?>) val) {
                checkAnnotationValue(obj);
            }
        }
    }

    private void check(MethodEntry method, ImportWalker walker) {
        for (Annotation anno : method.getAnnotations()) {
            check(anno);
        }
        if (!method.isAbstract()) {
            method.getInstructions().accept(walker);
        }
        check(method.getReturnType());
        for (TypeSignature param : method.getParamTypes()) {
            check(param);
        }
        for (TypeSignature ex : method.getMethodSignature().getThrowsSignature()) {
            check(ex);
        }
        for (TypeParameter arg : method.getMethodSignature().getTypeParameters()) {
            if (arg.getClassBound() != null) {
                check(arg.getClassBound());
            }
            for (TypeSignature sig : arg.getInterfaceBounds()) {
                check(sig);
            }
        }
    }

    private void check(FieldEntry field) {
        for (Annotation anno : field.getAnnotations()) {
            check(anno);
        }
        check(field.getType());
        // Field initializer is still within the ctor and will be walked with
        // the methods
    }

    void check(TypeSignature sig) {
        if (sig instanceof ClassTypeSignature) {
            ClassTypeSignature cls = (ClassTypeSignature) sig;
            add(cls.getDescriptor());
        } else if (sig instanceof GenericClassTypeSignature) {
            GenericClassTypeSignature cls = (GenericClassTypeSignature) sig;
            add(cls.getDescriptor());
            for (TypeArgument param : cls.getArguments()) {
                check(param.getSignature());
            }
        }
    }

    /**
     * Checks if the given type is imported.
     */
    public boolean checkImport(String type) {
        if (type.indexOf('$') != -1) {
            type = type.substring(0, type.indexOf('$'));
        }
        if (TypeHelper.isPrimative(type)) {
            return true;
        }
        for (String implicit : this.implicit_imports) {
            if (type.startsWith(implicit)) {
                return true;
            }
        }
        return this.imports.contains(type);
    }

    /**
     * Emits the imports to the given emitter context.
     */
    public void emitImports(JavaEmitterContext ctx) {
        for (Iterator<String> it = this.imports.iterator(); it.hasNext();) {
            String i = it.next();
            if (i.equals(ctx.getOuterType().getName())) {
                it.remove();
            }
        }
        List<String> imports = Lists.newArrayList(this.imports);
        for (int i = 0; i < ctx.getFormat().import_order.size(); i++) {
            String group = ctx.getFormat().import_order.get(i);
            if (group.startsWith("/#")) {
                // don't have static imports yet
                continue;
            }
            List<String> group_imports = Lists.newArrayList();
            for (Iterator<String> it = imports.iterator(); it.hasNext();) {
                String import_ = it.next();
                if (import_.startsWith(group)) {
                    group_imports.add(import_);
                    it.remove();
                }
            }
            Collections.sort(group_imports);
            for (String import_ : group_imports) {
                ctx.printString("import ");
                ctx.printString(import_.replace('/', '.'));
                if (ctx.usesSemicolons()) {
                    ctx.printString(";");
                }
                ctx.newLine();
            }
            if (!group_imports.isEmpty() && i < ctx.getFormat().import_order.size() - 1) {
                for (int o = 0; o < ctx.getFormat().blank_lines_between_import_groups; o++) {
                    ctx.newLine();
                }
            }
        }
        for (int i = 0; i < ctx.getFormat().blank_lines_after_imports - 1; i++) {
            ctx.newLine();
        }
    }

    /**
     * A visitor to gather types needing to be imported.
     */
    private class ImportWalker implements InstructionVisitor {

        public ImportWalker() {
        }

        @Override
        public void visitCast(Cast cast) {
            ImportManager.this.check(cast.getType());
        }

        @Override
        public void visitLocalInstance(LocalInstance local) {
            ImportManager.this.check(local.getType());
        }

        @Override
        public void visitTypeConstant(TypeConstant cst) {
            ImportManager.this.add(cst.getConstant().getDescriptor());
        }

        @Override
        public void visitNew(New ne) {
            ImportManager.this.check(ne.getType());
        }

        @Override
        public void visitArrayAccess(ArrayAccess insn) {
        }

        @Override
        public void visitDoubleConstant(DoubleConstant insn) {
        }

        @Override
        public void visitDynamicInvoke(DynamicInvoke insn) {
        }

        @Override
        public void visitFloatConstant(FloatConstant insn) {
        }

        @Override
        public void visitInstanceFieldAccess(InstanceFieldAccess insn) {
        }

        @Override
        public void visitInstanceMethodInvoke(InstanceMethodInvoke insn) {
        }

        @Override
        public void visitInstanceOf(InstanceOf insn) {
        }

        @Override
        public void visitIntConstant(IntConstant insn) {
        }

        @Override
        public void visitLocalAccess(LocalAccess insn) {
        }

        @Override
        public void visitLongConstant(LongConstant insn) {
        }

        @Override
        public void visitNegativeOperator(NegativeOperator insn) {
        }

        @Override
        public void visitNewArray(NewArray insn) {
        }

        @Override
        public void visitNullConstant(NullConstant insn) {
        }

        @Override
        public void visitNumberCompare(NumberCompare insn) {
        }

        @Override
        public void visitOperator(Operator insn) {
        }

        @Override
        public void visitStaticFieldAccess(StaticFieldAccess insn) {
        }

        @Override
        public void visitStaticMethodInvoke(StaticMethodInvoke insn) {
        }

        @Override
        public void visitStringConstant(StringConstant insn) {
        }

        @Override
        public void visitTernary(Ternary insn) {
        }

        @Override
        public void visitMultiNewArray(MultiNewArray insn) {
            // TODO Auto-generated method stub
            
        }

    }

}
