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
package org.spongepowered.despector.emitter.kotlin.type;

import org.spongepowered.despector.ast.Annotation;
import org.spongepowered.despector.ast.insn.Instruction;
import org.spongepowered.despector.ast.stmt.Statement;
import org.spongepowered.despector.ast.stmt.assign.FieldAssignment;
import org.spongepowered.despector.ast.stmt.assign.StaticFieldAssignment;
import org.spongepowered.despector.ast.type.ClassEntry;
import org.spongepowered.despector.ast.type.FieldEntry;
import org.spongepowered.despector.ast.type.MethodEntry;
import org.spongepowered.despector.ast.type.TypeEntry;
import org.spongepowered.despector.ast.type.TypeEntry.InnerClassInfo;
import org.spongepowered.despector.config.ConfigManager;
import org.spongepowered.despector.emitter.AstEmitter;
import org.spongepowered.despector.emitter.java.JavaEmitterContext;
import org.spongepowered.despector.emitter.java.special.GenericsEmitter;
import org.spongepowered.despector.emitter.java.type.FieldEntryEmitter;
import org.spongepowered.despector.emitter.kotlin.special.KotlinCompanionClassEmitter;
import org.spongepowered.despector.emitter.kotlin.special.KotlinDataClassEmitter;
import org.spongepowered.despector.util.AstUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An emitter for kotlin casts.
 */
public class KotlinClassEntryEmitter implements AstEmitter<JavaEmitterContext, ClassEntry> {

    private static final Set<String> HIDDEN_ANNOTATIONS = new HashSet<>();

    static {
        HIDDEN_ANNOTATIONS.add("Lkotlin/Metadata;");
    }

    @Override
    public boolean emit(JavaEmitterContext ctx, ClassEntry type) {

        if (type.getStaticMethod("copy$default") != null) {
            KotlinDataClassEmitter data_emitter = ctx.getEmitterSet().getSpecialEmitter(KotlinDataClassEmitter.class);
            data_emitter.emit(ctx, type);
            return true;
        }

        if (type.getName().endsWith("$Companion")) {
            KotlinCompanionClassEmitter companion_emitter = ctx.getEmitterSet().getSpecialEmitter(KotlinCompanionClassEmitter.class);
            companion_emitter.emit(ctx, type);
            return true;
        }

        boolean emit_class = !type.getMethods().isEmpty() || !type.getFields().isEmpty();

        if (emit_class) {
            for (Annotation anno : type.getAnnotations()) {
                if (HIDDEN_ANNOTATIONS.contains(anno.getType().getName())) {
                    continue;
                }
                ctx.printIndentation();
                ctx.emit(anno);
                ctx.newLine();
            }
            ctx.printIndentation();
            InnerClassInfo inner_info = null;
            if (type.isInnerClass() && ctx.getOuterType() != null) {
                inner_info = ctx.getOuterType().getInnerClassInfo(type.getName());
            }
            ctx.printString("class ");
            if (inner_info != null) {
                ctx.printString(inner_info.getSimpleName());
            } else {
                String name = type.getName().replace('/', '.');
                if (name.indexOf('.') != -1) {
                    name = name.substring(name.lastIndexOf('.') + 1, name.length());
                }
                name = name.replace('$', '.');
                ctx.printString(name);
            }

            GenericsEmitter generics = ctx.getEmitterSet().getSpecialEmitter(GenericsEmitter.class);
            if (type.getSignature() != null) {
                generics.emitTypeParameters(ctx, type.getSignature().getParameters());
            }

            if (!type.getSuperclass().equals("Ljava/lang/Object;")) {
                ctx.printString(" extends ");
                ctx.emitTypeName(type.getSuperclassName());
                if (type.getSignature() != null && type.getSignature().getSuperclassSignature() != null) {
                    generics.emitTypeArguments(ctx, type.getSignature().getSuperclassSignature().getArguments());
                }
            }
            if (!type.getInterfaces().isEmpty()) {
                ctx.printString(" : ");
                for (int i = 0; i < type.getInterfaces().size(); i++) {
                    ctx.emitType(type.getInterfaces().get(i));
                    generics.emitTypeArguments(ctx, type.getSignature().getInterfaceSignatures().get(i).getArguments());
                    if (i < type.getInterfaces().size() - 1) {
                        ctx.printString(" ", ctx.getFormat().insert_space_before_comma_in_superinterfaces);
                        ctx.printString(",");
                        ctx.printString(" ", ctx.getFormat().insert_space_after_comma_in_superinterfaces);
                    }
                }
            }
            ctx.printString(" ", ctx.getFormat().insert_space_before_opening_brace_in_type_declaration);
            ctx.printString("{");
            ctx.newLine(ctx.getFormat().blank_lines_before_first_class_body_declaration + 1);

            // Ordering is static fields -> static methods -> instance fields ->
            // instance methods
            ctx.indent();
        }
        emitStaticFields(ctx, type);
        emitStaticMethods(ctx, type);
        emitFields(ctx, type);
        emitMethods(ctx, type);

        Collection<InnerClassInfo> inners = type.getInnerClasses();
        for (InnerClassInfo inner : inners) {
            if (inner.getOuterName() == null || !inner.getOuterName().equals(type.getName())) {
                continue;
            }
            TypeEntry inner_type = type.getSource().get(inner.getName());
            ctx.emit(inner_type);
            ctx.newLine();
        }
        if (emit_class) {
            ctx.dedent();
            ctx.printIndentation();
            ctx.printString("}");
            ctx.newLine();
        }
        return true;
    }

    /**
     * Emits the static fields of the given type.
     */
    public void emitStaticFields(JavaEmitterContext ctx, ClassEntry type) {
        if (!type.getStaticFields().isEmpty()) {

            Map<String, Instruction> static_initializers = new HashMap<>();

            MethodEntry static_init = type.getStaticMethod("<clinit>");
            if (static_init != null && static_init.getInstructions() != null) {
                for (Statement stmt : static_init.getInstructions().getStatements()) {
                    if (!(stmt instanceof StaticFieldAssignment)) {
                        break;
                    }
                    StaticFieldAssignment assign = (StaticFieldAssignment) stmt;
                    if (!assign.getOwnerName().equals(type.getName())) {
                        break;
                    }
                    static_initializers.put(assign.getFieldName(), assign.getValue());
                }
            }

            boolean at_least_one = false;
            for (FieldEntry field : type.getStaticFields()) {
                if (field.isSynthetic() || field.getName().equals("Companion")) {
                    if (ConfigManager.getConfig().emitter.emit_synthetics) {
                        ctx.printIndentation();
                        ctx.printString("// Synthetic");
                        ctx.newLine();
                    } else {
                        continue;
                    }
                }
                at_least_one = true;
                ctx.printIndentation();
                ctx.emit(field);
                if (static_initializers.containsKey(field.getName())) {
                    ctx.printString(" = ");
                    ctx.emit(static_initializers.get(field.getName()), field.getType());
                }
                ctx.printString(";");
                ctx.newLine();
            }
            if (at_least_one) {
                ctx.newLine();
            }
        }
    }

    /**
     * Emits the static methods of the given type.
     */
    public void emitStaticMethods(JavaEmitterContext ctx, ClassEntry type) {
        if (!type.getStaticMethods().isEmpty()) {
            for (MethodEntry mth : type.getStaticMethods()) {
                if (mth.isSynthetic()) {
                    if (ConfigManager.getConfig().emitter.emit_synthetics) {
                        ctx.printIndentation();
                        ctx.printString("// Synthetic");
                        ctx.newLine();
                    } else {
                        continue;
                    }
                }
                if (ctx.emit(mth)) {
                    ctx.newLine();
                    ctx.newLine();
                }
            }
        }
    }

    /**
     * Emits the instance fields of the given type.
     */
    public void emitFields(JavaEmitterContext ctx, ClassEntry type) {
        if (!type.getFields().isEmpty()) {

            Map<FieldEntry, Instruction> initializers = new HashMap<>();
            List<MethodEntry> inits = type.getMethods().stream().filter((m) -> m.getName().equals("<init>")).collect(Collectors.toList());
            MethodEntry main = null;
            if (inits.size() == 1) {
                main = inits.get(0);
            }
            if (main != null && main.getInstructions() != null) {
                for (int i = 1; i < main.getInstructions().getStatements().size(); i++) {
                    Statement next = main.getInstructions().getStatements().get(i);
                    if (!(next instanceof FieldAssignment)) {
                        break;
                    }
                    FieldAssignment assign = (FieldAssignment) next;
                    if (!type.getName().equals(assign.getOwnerName())) {
                        break;
                    }
                    if (AstUtil.references(assign.getValue(), null)) {
                        break;
                    }
                    assign.setInitializer(true);
                    FieldEntry fld = type.getField(assign.getFieldName());
                    initializers.put(fld, assign.getValue());
                }
            }

            boolean at_least_one = false;
            for (FieldEntry field : type.getFields()) {
                if (field.isSynthetic()) {
                    if (ConfigManager.getConfig().emitter.emit_synthetics) {
                        ctx.printIndentation();
                        ctx.printString("// Synthetic");
                        ctx.newLine();
                    } else {
                        continue;
                    }
                }
                at_least_one = true;
                ctx.printIndentation();
                ctx.emit(field);
                Instruction init = initializers.get(field);
                if (init != null) {
                    ctx.setField(field);
                    if (ctx.getFormat().align_type_members_on_columns && ctx.getType() != null) {
                        int len = FieldEntryEmitter.getMaxNameLength(ctx, ctx.getType().getFields());
                        len -= field.getName().length();
                        len++;
                        for (int i = 0; i < len; i++) {
                            ctx.printString(" ");
                        }
                    } else {
                        ctx.printString(" ");
                    }
                    ctx.printString("= ");
                    ctx.emit(init, field.getType());
                    ctx.setField(null);
                }
                ctx.printString(";");
                ctx.newLine();
            }
            if (at_least_one) {
                ctx.newLine();
            }
        }
    }

    /**
     * Emits the instance methods of the given type.
     */
    public void emitMethods(JavaEmitterContext ctx, ClassEntry type) {
        if (!type.getMethods().isEmpty()) {
            for (MethodEntry mth : type.getMethods()) {
                if (mth.isSynthetic() || mth.getName().equals("<init>")) {
                    if (ConfigManager.getConfig().emitter.emit_synthetics) {
                        ctx.printIndentation();
                        ctx.printString("// Synthetic");
                        ctx.newLine();
                    } else {
                        continue;
                    }
                }
                if (ctx.emit(mth)) {
                    ctx.newLine();
                    ctx.newLine();
                }
            }
        }
    }

}
