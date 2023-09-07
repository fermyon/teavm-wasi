/*
 *  Copyright 2022 TeaVM Contributors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.interop;

// This class implements a very naive allocator, useful for allocating buffers
// which are outside the Java heap and therefore guaranteed not to move.
//
// The implementation uses a simple bit map to track which 8-byte chunks of
// memory are allocated.  It's not particularly efficient but suffices for
// simple use cases.
//
// For more information about the API, see
// https://github.com/WebAssembly/component-model/blob/main/design/mvp/CanonicalABI.
public final class Memory {
    private Memory() {
    }

    private static final int MAP_SIZE = 2048;
    public static final int HEAP_OFFSET = 65 * MAP_SIZE;

    private static int ceilingDivide(int n, int d) {
        return (n + d - 1) / d;
    }

    // Obsolete component model function, provided for backwards compatibility:
    @Export(name = "canonical_abi_realloc")
    public static Address oldRealloc(Address oldAddress, int oldSize, int align, int newSize) {
        return realloc(oldAddress, oldSize, align, newSize);
    }

    // Obsolete component model function, provided for backwards compatibility:
    @Export(name = "canonical_abi_free")
    public static void oldFree(Address address, int size, int align) {
        free(address, size, align);
    }

    @Export(name = "cabi_realloc")
    public static Address realloc(Address oldAddress, int oldSize, int align, int newSize) {
        if (oldSize < 0 || newSize < 0 || !(align == 1 || align == 2 || align == 4 || align == 8)) {
            throw new IllegalArgumentException();
        }

        if (oldSize != 0) {
            free(oldAddress, oldSize, align);
        }

        if (newSize == 0) {
            return Address.align(Address.fromInt(HEAP_OFFSET), align);
        }

        if (newSize < MAP_SIZE) {
            int needed = ceilingDivide(newSize, 8);
            long mask = ~(0xFFFFFFFFFFFFFFFFL << needed);
            for (int i = 0; i < MAP_SIZE / 8; ++i) {
                long entry = Address.fromInt(i * 8).getLong();
                if (entry != 0xFFFFFFFFFFFFFFFFL) {
                    int count = 0;
                    for (int j = 0; j < 64; ++j) {
                        if (((entry >>> j) & 1) == 0) {
                            count += 1;

                            if (count == needed) {
                                int start = (j + 1) - count;
                                Address.fromInt(i * 8).putLong(entry | (mask << start));

                                if (MAP_SIZE + (((i * 64) + start) * 8) + newSize > HEAP_OFFSET) {
                                    throw new AssertionError();
                                }

                                return Address.fromInt(MAP_SIZE + (((i * 64) + start) * 8));
                            }
                        } else {
                            count = 0;
                        }
                    }
                }
            }
        } else {
            int needed = ceilingDivide(newSize, MAP_SIZE);
            int count = 0;
            for (int i = 0; i < MAP_SIZE / 8; ++i) {
                long entry = Address.fromInt(i * 8).getLong();
                if (entry == 0) {
                    count += 1;

                    if (count == needed) {
                        int start = (i + 1) - count;
                        for (int j = start; j <= i; ++j) {
                            Address.fromInt(j * 8).putLong(0xFFFFFFFFFFFFFFFFL);
                        }

                        if (MAP_SIZE + (start * 64 * 8) + newSize > HEAP_OFFSET) {
                            throw new AssertionError();
                        }

                        return Address.fromInt(MAP_SIZE + (start * 64 * 8));
                    }
                } else {
                    count = 0;
                }
            }
        }

        throw new OutOfMemoryError();
    }

    public static Address malloc(int size, int align) {
        return realloc(Address.fromInt(0), 0, align, size);
    }

    public static void free(Address address, int size, int align) {
        if (size < 0 || !(align == 1 || align == 2 || align == 4 || align == 8)) {
            throw new IllegalArgumentException();
        }

        if (size == 0) {
            return;
        }

        if (address.toInt() < MAP_SIZE || address.toInt() + size > HEAP_OFFSET) {
            throw new IllegalArgumentException();
        }

        int offset = (address.toInt() - MAP_SIZE) / 8;
        if (size < MAP_SIZE) {
            int needed = ceilingDivide(size, 8);
            long mask = ~(0xFFFFFFFFFFFFFFFFL << needed);
            Address mapAddress = Address.fromInt((offset / 64) * 8);

            long entry = mapAddress.getLong();
            if (((entry >>> (offset % 64)) & mask) != mask) {
                throw new IllegalArgumentException();
            }
            mapAddress.putLong(entry & ~(mask << (offset % 64)));
        } else {
            int needed = ceilingDivide(size, MAP_SIZE);
            for (int i = 0; i < needed; ++i) {
                Address mapAddress = Address.fromInt((offset / 8) + (i * 8));
                long entry = mapAddress.getLong();
                if (entry != 0xFFFFFFFFFFFFFFFFL) {
                    throw new IllegalArgumentException();
                }
                mapAddress.putLong(0);
            }
        }
    }

    // TODO: make this an Address intrinsic that does a bulk memory operation
    public static void getBytes(Address address, byte[] bytes, int offset, int length) {
        for (int i = 0; i < length; ++i) {
            bytes[offset + i] = address.add(i).getByte();
        }
    }

    // TODO: make this an Address intrinsic that does a bulk memory operation
    public static void putBytes(Address address, byte[] bytes, int offset, int length) {
        for (int i = 0; i < length; ++i) {
            address.add(i).putByte(bytes[offset + i]);
        }
    }

    // TODO: make this an Address intrinsic that does a bulk memory operation
    public static void getShorts(Address address, short[] shorts, int offset, int length) {
        for (int i = 0; i < length; ++i) {
            shorts[offset + i] = address.add(i * 2).getShort();
        }
    }

    // TODO: make this an Address intrinsic that does a bulk memory operation
    public static void putShorts(Address address, short[] shorts, int offset, int length) {
        for (int i = 0; i < length; ++i) {
            address.add(i * 2).putShort(shorts[offset + i]);
        }
    }

    // TODO: make this an Address intrinsic that does a bulk memory operation
    public static void getInts(Address address, int[] ints, int offset, int length) {
        for (int i = 0; i < length; ++i) {
            ints[offset + i] = address.add(i * 4).getInt();
        }
    }

    // TODO: make this an Address intrinsic that does a bulk memory operation
    public static void putInts(Address address, int[] ints, int offset, int length) {
        for (int i = 0; i < length; ++i) {
            address.add(i * 4).putInt(ints[offset + i]);
        }
    }

    // TODO: make this an Address intrinsic that does a bulk memory operation
    public static void getLongs(Address address, long[] longs, int offset, int length) {
        for (int i = 0; i < length; ++i) {
            longs[offset + i] = address.add(i * 8).getLong();
        }
    }

    // TODO: make this an Address intrinsic that does a bulk memory operation
    public static void putLongs(Address address, long[] longs, int offset, int length) {
        for (int i = 0; i < length; ++i) {
            address.add(i * 8).putLong(longs[offset + i]);
        }
    }

    // TODO: make this an Address intrinsic that does a bulk memory operation
    public static void getFloats(Address address, float[] floats, int offset, int length) {
        for (int i = 0; i < length; ++i) {
            floats[offset + i] = address.add(i * 4).getFloat();
        }
    }

    // TODO: make this an Address intrinsic that does a bulk memory operation
    public static void putFloats(Address address, float[] floats, int offset, int length) {
        for (int i = 0; i < length; ++i) {
            address.add(i * 4).putFloat(floats[offset + i]);
        }
    }

    // TODO: make this an Address intrinsic that does a bulk memory operation
    public static void getDoubles(Address address, double[] doubles, int offset, int length) {
        for (int i = 0; i < length; ++i) {
            doubles[offset + i] = address.add(i * 8).getDouble();
        }
    }

    // TODO: make this an Address intrinsic that does a bulk memory operation
    public static void putDoubles(Address address, double[] doubles, int offset, int length) {
        for (int i = 0; i < length; ++i) {
            address.add(i * 8).putDouble(doubles[offset + i]);
        }
    }
}
