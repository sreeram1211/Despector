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
package org.spongepowered.despector.transform.matcher.instruction;

import org.spongepowered.despector.ast.Locals.LocalInstance;
import org.spongepowered.despector.ast.insn.Instruction;
import org.spongepowered.despector.ast.insn.var.LocalAccess;
import org.spongepowered.despector.transform.matcher.InstructionMatcher;
import org.spongepowered.despector.transform.matcher.MatchContext;

import java.util.function.Function;

/**
 * A matcher for local accesses.
 */
public class LocalAccessMatcher implements InstructionMatcher<LocalAccess> {

    private LocalInstance local;
    private Function<MatchContext, LocalInstance> ctx_local;

    LocalAccessMatcher(LocalInstance local) {
        this.local = local;
    }

    LocalAccessMatcher(Function<MatchContext, LocalInstance> local) {
        this.ctx_local = local;
    }

    @Override
    public LocalAccess match(MatchContext ctx, Instruction insn) {
        if (!(insn instanceof LocalAccess)) {
            return null;
        }
        LocalAccess acc = (LocalAccess) insn;
        if (this.local != null) {
            if (!this.local.equals(acc.getLocal())) {
                return null;
            }
        } else if (this.ctx_local != null) {
            LocalInstance l = this.ctx_local.apply(ctx);
            if (l == null || !l.equals(acc.getLocal())) {
                return null;
            }
        }
        return acc;
    }

    /**
     * A matcher builder.
     */
    public static class Builder {

        private LocalInstance local;
        private Function<MatchContext, LocalInstance> ctx_local;

        public Builder() {
            reset();
        }

        public Builder local(LocalInstance local) {
            this.local = local;
            return this;
        }

        public Builder fromContext(String identifier) {
            this.ctx_local = (ctx) -> ctx.getLocal(identifier);
            return this;
        }

        /**
         * Resets this builder.
         */
        public Builder reset() {
            this.local = null;
            this.ctx_local = null;
            return this;
        }

        /**
         * Creates a new matcher.
         */
        public LocalAccessMatcher build() {
            if (this.ctx_local != null) {
                return new LocalAccessMatcher(this.ctx_local);
            }
            return new LocalAccessMatcher(this.local);
        }

    }

}
