/*
 * This file is part of Mixin, licensed under the MIT License (MIT).
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
package org.spongepowered.asm.util;

import com.google.common.base.Predicate;
import com.google.common.collect.UnmodifiableListIterator;

import java.util.ListIterator;

import static com.google.common.base.Preconditions.checkNotNull;

// This file is added by us at Moonsworth
public final class ListIterators {

    private ListIterators() {}

    public static <T> UnmodifiableListIterator<T> filter(final ListIterator<T> unfiltered, final Predicate<? super T> predicate) {
        checkNotNull(unfiltered);
        checkNotNull(predicate);

        return new UnmodifiableListIterator<>() {
            private int index = 0;

            private boolean hasNext = false;
            private boolean hasPrevious = false;

            private T next = this.computeNext();
            private T previous = null;

            @Override
            public boolean hasNext() {
                return this.hasNext;
            }

            @Override
            public boolean hasPrevious() {
                return this.hasPrevious;
            }

            @Override
            public int nextIndex() {
                return this.index + 1;
            }

            @Override
            public int previousIndex() {
                return this.index - 1;
            }

            @Override
            public T next() {
                T value = this.next;

                this.next = this.computeNext();
                this.hasPrevious = true;
                this.previous = value;
                ++this.index;

                return value;
            }

            @Override
            public T previous() {
                T value = this.previous;

                this.previous = this.computePrevious();
                this.hasNext = true;
                this.next = value;
                --this.index;

                return value;
            }

            private T computeNext() {
                while (unfiltered.hasNext()) {
                    T element = unfiltered.next();

                    if (predicate.apply(element)) {
                        this.hasNext = true;
                        return element;
                    }
                }

                this.hasNext = false;
                return null;
            }

            private T computePrevious() {
                while (unfiltered.hasPrevious()) {
                    T element = unfiltered.previous();

                    if (predicate.apply(element)) {
                        this.hasPrevious = true;
                        return element;
                    }
                }

                this.hasPrevious = false;
                return null;
            }
        };
    }
}
