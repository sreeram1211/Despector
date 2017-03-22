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
package org.spongepowered.despector.decompiler.method.graph.region;

import org.spongepowered.despector.ast.members.insn.branch.condition.Condition;
import org.spongepowered.despector.decompiler.method.ConditionBuilder;
import org.spongepowered.despector.decompiler.method.PartialMethod;
import org.spongepowered.despector.decompiler.method.graph.RegionProcessor;
import org.spongepowered.despector.decompiler.method.graph.data.BlockSection;
import org.spongepowered.despector.decompiler.method.graph.data.InlineBlockSection;
import org.spongepowered.despector.decompiler.method.graph.data.OpcodeBlock;
import org.spongepowered.despector.decompiler.method.graph.data.WhileBlockSection;

import java.util.ArrayList;
import java.util.List;

/**
 * A region processor that checks if the region forms a while loop.
 */
public class WhileRegionProcessor implements RegionProcessor {

    @Override
    public BlockSection process(PartialMethod partial, List<OpcodeBlock> region, OpcodeBlock ret, int body_start) {
        OpcodeBlock start = region.get(0);
        if (start.isGoto()) {
            List<OpcodeBlock> condition_blocks = new ArrayList<>();
            OpcodeBlock next = start.getTarget();
            int pos = region.indexOf(next);
            int cond_start = pos;
            while (next.isConditional()) {
                condition_blocks.add(next);
                pos++;
                if (pos >= region.size()) {
                    break;
                }
                next = region.get(pos);
            }

            OpcodeBlock body = region.get(1);
            Condition cond = ConditionBuilder.makeCondition(condition_blocks, partial.getLocals(), body, ret);

            WhileBlockSection section = new WhileBlockSection(cond);

            for (int i = 1; i < cond_start; i++) {
                next = region.get(i);
                if (next.hasPrecompiledSection()) {
                    section.appendBody(next.getPrecompiledSection());
                } else if (next.isConditional()) {
                    // If we encounter another conditional block then its an
                    // error
                    // as we should have already processed all sub regions
                    throw new IllegalStateException("Unexpected conditional when building if body");
                } else {
                    section.appendBody(new InlineBlockSection(next));
                }
            }

            return section;
        }
        return null;
    }

}