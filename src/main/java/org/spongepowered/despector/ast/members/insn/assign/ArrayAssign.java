/*
 * The MIT License (MIT)
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
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
package org.spongepowered.despector.ast.members.insn.assign;

import org.spongepowered.despector.ast.members.insn.InstructionVisitor;
import org.spongepowered.despector.ast.members.insn.arg.Instruction;

/**
 * An array assignment statement.
 * 
 * <p>Example: {@code var[i] = val;}</p>
 */
public class ArrayAssign extends Assignment {

    private final Instruction array;
    private final Instruction index;

    public ArrayAssign(Instruction array, Instruction index, Instruction val) {
        super(val);
        this.index = index;
        this.array = array;
    }

    public Instruction getArray() {
        return this.array;
    }

    public Instruction getIndex() {
        return this.index;
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visitArrayAssign(this);
        this.array.accept(visitor);
        this.index.accept(visitor);
        this.val.accept(visitor);
    }

    @Override
    public String toString() {
        return this.array.toString() + "[" + this.index.toString() + "] = " + this.val.toString() + ";";
    }

}
