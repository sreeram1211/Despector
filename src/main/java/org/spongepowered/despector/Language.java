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
package org.spongepowered.despector;

import org.spongepowered.despector.ast.type.TypeEntry;
import org.spongepowered.despector.decompiler.Decompiler;
import org.spongepowered.despector.decompiler.Decompilers;
import org.spongepowered.despector.emitter.Emitter;
import org.spongepowered.despector.emitter.Emitters;

import java.util.function.Function;

public enum Language {

    JAVA(Decompilers.JAVA, (t)->".java", Emitters.JAVA),
    KOTLIN(Decompilers.KOTLIN, (t)->".kt", Emitters.KOTLIN),
    ANY(Decompilers.WILD, (t)->t.getLanguage().getFileExt(t), Emitters.WILD);

    private final Decompiler decompiler;
    private final Function<TypeEntry, String> ext;
    private final Emitter emitter;

    Language(Decompiler decomp, Function<TypeEntry, String> ext, Emitter emitter) {
        this.decompiler = decomp;
        this.ext = ext;
        this.emitter = emitter;
    }

    public Decompiler getDecompiler() {
        return this.decompiler;
    }

    public String getFileExt(TypeEntry type) {
        return this.ext.apply(type);
    }

    public Emitter getEmitter() {
        return this.emitter;
    }
}
