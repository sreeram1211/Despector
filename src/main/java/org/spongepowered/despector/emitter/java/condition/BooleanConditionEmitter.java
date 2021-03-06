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
package org.spongepowered.despector.emitter.java.condition;

import org.spongepowered.despector.ast.generic.ClassTypeSignature;
import org.spongepowered.despector.ast.insn.Instruction;
import org.spongepowered.despector.ast.insn.condition.BooleanCondition;
import org.spongepowered.despector.ast.insn.cst.IntConstant;
import org.spongepowered.despector.ast.insn.misc.InstanceOf;
import org.spongepowered.despector.ast.insn.misc.Ternary;
import org.spongepowered.despector.ast.insn.op.Operator;
import org.spongepowered.despector.emitter.ConditionEmitter;
import org.spongepowered.despector.emitter.InstructionEmitter;
import org.spongepowered.despector.emitter.java.JavaEmitterContext;
import org.spongepowered.despector.emitter.java.instruction.TernaryEmitter;

/**
 * An emitter for boolean conditions.
 */
public class BooleanConditionEmitter implements ConditionEmitter<JavaEmitterContext, BooleanCondition> {

    @Override
    public void emit(JavaEmitterContext ctx, BooleanCondition bool) {
        if (checkConstant(ctx, bool)) {
            return;
        }
        if (bool.isInverse()) {
            ctx.printString("!");
        }
        emit(ctx, bool.getConditionValue(), bool.isInverse());
    }

    protected void emit(JavaEmitterContext ctx, Instruction val, boolean parens_needed) {
        if (parens_needed && val instanceof InstanceOf) {
            ctx.printString("(");
            ctx.emit(val, ClassTypeSignature.BOOLEAN);
            ctx.printString(")");
        } else {
            ctx.emit(val, ClassTypeSignature.BOOLEAN);
        }
    }

    protected boolean checkConstant(JavaEmitterContext ctx, BooleanCondition bool) {
        Instruction val = bool.getConditionValue();
        if (val.inferType() == ClassTypeSignature.INT) {
            if (val instanceof IntConstant) {
                IntConstant cst = (IntConstant) val;
                if (cst.getConstant() == 0) {
                    ctx.printString("false");
                } else {
                    ctx.printString("true");
                }
                return true;
            }
            if (val instanceof Ternary) {
                Ternary ternary = (Ternary) val;
                InstructionEmitter<JavaEmitterContext, Ternary> emitter = ctx.getEmitterSet().getInstructionEmitter(Ternary.class);
                if (emitter instanceof TernaryEmitter) {
                    if (((TernaryEmitter) emitter).checkBooleanExpression(ctx, ternary)) {
                        return true;
                    }
                }
            }
            if (val instanceof Operator) {
                ctx.printString("(");
                ctx.emit(val, ClassTypeSignature.INT);
                ctx.printString(")");
            } else {
                ctx.emit(val, ClassTypeSignature.INT);
            }
            if (bool.isInverse()) {
                ctx.printString(" == 0");
            } else {
                ctx.printString(" != 0");
            }
            return true;
        }
        return false;
    }

}
