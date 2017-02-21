/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 KuiGang Wang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.lattelang;

import groovy.lang.Closure;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.file.SourceDirectorySetFactory;
import org.gradle.util.ConfigureUtil;

/**
 * default latte source set
 */
public class DefaultLatteSourceSet implements LatteSourceSet {
        private final SourceDirectorySet latte;
        private final SourceDirectorySet allLatte;

        public DefaultLatteSourceSet(String displayName, SourceDirectorySetFactory sourceDirectorySetFactory) {
                latte = sourceDirectorySetFactory.create(displayName + " Latte source");
                latte.getFilter().include("**/*.lt", "**/*.latte");
                allLatte = sourceDirectorySetFactory.create(displayName + " Latte source");
                allLatte.source(latte);
                allLatte.getFilter().include("**/*.lt", "**/*.latte");
        }

        @Override
        public SourceDirectorySet getLatte() {
                return latte;
        }

        @Override
        public LatteSourceSet groovy(Closure configureClosure) {
                ConfigureUtil.configure(configureClosure, getLatte());
                return this;
        }

        @Override
        public SourceDirectorySet getAllLatte() {
                return allLatte;
        }
}
