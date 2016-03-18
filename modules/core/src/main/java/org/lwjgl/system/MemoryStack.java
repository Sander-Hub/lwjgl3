/*
 * Copyright LWJGL. All rights reserved.
 * License terms: http://lwjgl.org/license.php
 */
package org.lwjgl.system;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;

import java.nio.*;
import java.util.Arrays;

import static org.lwjgl.system.Checks.*;
import static org.lwjgl.system.MathUtil.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.system.Pointer.*;
import static org.lwjgl.system.ThreadLocalUtil.*;

/**
 * An off-heap memory stack.
 *
 * <p>This class should be used in a thread-local manner for stack allocations.</p>
 */
public class MemoryStack {

	private static final int DEFAULT_STACK_SIZE = Configuration.STACK_SIZE.get(32) * 1024;

	static {
		if ( DEFAULT_STACK_SIZE < 0 )
			throw new IllegalStateException("Invalid stack size.");
	}

	private final ByteBuffer buffer;

	private final long address;

	private final int size;

	private int pointer;

	private int[] frames;
	private int   frameIndex;

	/** Creates a new {@link MemoryStack} with the default size. */
	public MemoryStack() {
		this(DEFAULT_STACK_SIZE);
	}

	/**
	 * Creates a new {@link MemoryStack} with the specified size.
	 *
	 * @param size the maximum number of bytes that may be allocated on the stack
	 */
	public MemoryStack(int size) {
		this.buffer = BufferUtils.createByteBuffer(size);
		this.address = memAddress(buffer);

		this.size = size;
		this.pointer = size;

		this.frames = new int[16];
	}

	/** Returns the stack of the current thread. */
	public static MemoryStack stackGet() {
		return tlsGet().stack;
	}

	/**
	 * Calls {@link #push} on the stack of the current thread.
	 *
	 * @return the stack of the current thread.
	 */
	public static MemoryStack stackPush() {
		return tlsGet().stack.push();
	}

	/**
	 * Calls {@link #pop} on the stack of the current thread.
	 *
	 * @return the stack of the current thread.
	 */
	public static MemoryStack stackPop() {
		return tlsGet().stack.pop();
	}

	/**
	 * Stores the current stack pointer and pushes a new frame to the stack.
	 *
	 * <p>This method should be called when entering a method, before doing any stack allocations. When exiting a method, call the {@link #pop} method to
	 * restore the previous stack frame.</p>
	 *
	 * <p>Pairs of push/pop calls may be nested. Care must be taken to:</p>
	 * <ul>
	 * <li>match every push with a pop</li>
	 * <li>not call pop before push has been called at least once</li>
	 * <li>not nest push calls to more than the maximum supported depth</li>
	 * </ul>
	 *
	 * @return this stack
	 */
	public MemoryStack push() {
		if ( frameIndex == frames.length )
			frames = Arrays.copyOf(frames, frames.length * 2);

		frames[frameIndex++] = pointer;
		return this;
	}

	/**
	 * Pops the current stack frame and moves the stack pointer to the end of the previous stack frame.
	 *
	 * @return this stack
	 */
	public MemoryStack pop() {
		pointer = frames[--frameIndex];
		return this;
	}

	/**
	 * Returns the address of the backing off-heap memory.
	 *
	 * <p>The stack grows "downwards", so the bottom of the stack is at {@code address + size}, while the top is at {@code address}.</p>
	 */
	public long getAddress() {
		return address;
	}

	/**
	 * Returns the size of the backing off-heap memory.
	 *
	 * <p>This is the maximum number of bytes that may be allocated on the stack.</p>
	 */
	public int getSize() {
		return size;
	}

	/**
	 * Returns the current frame index.
	 *
	 * <p>This is the current number of nested {@link #push} calls.</p>
	 */
	public int getFrameIndex() {
		return frameIndex;
	}

	/**
	 * Returns the current stack pointer.
	 *
	 * <p>The stack grows "downwards", so when the stack is empty {@code pointer} is equal to {@code size}. On every allocation {@code pointer} is reduced by
	 * the allocated size (after alignment) and {@code address + pointers} points to the last byte of the last allocation.</p>
	 *
	 * <p>Effectively, this methods returns how many more bytes may be allocated on the stack.</p>
	 */
	public int getPointer() {
		return pointer;
	}

	private static void checkAlignment(int alignment) {
		if ( !mathIsPoT(alignment) )
			throw new IllegalArgumentException("Alignment must be a power-of-two value.");
	}

	private static void checkPointer(int offset) {
		if ( offset < 0 )
			throw new OutOfMemoryError("Out of stack space.");
	}

	/**
	 * Calls {@link #nmalloc(int, int)} with {@code alignment} equal to 1.
	 *
	 * @param size the allocation size
	 *
	 * @return the memory address on the stack for the requested allocation
	 */
	public long nmalloc(int size) {
		return nmalloc(1, size);
	}

	/**
	 * Allocates a block of {@code size} bytes of memory on the stack. The content of the newly allocated block of memory is not initialized, remaining with
	 * indeterminate values.
	 *
	 * @param alignment the required alignment
	 * @param size      the allocation size
	 *
	 * @return the memory address on the stack for the requested allocation
	 */
	public long nmalloc(int alignment, int size) {
		if ( frameIndex == 0 )
			throw new IllegalStateException("A frame has not been pushed to the stack yet.");

		int newPointer = pointer - size;

		if ( CHECKS ) {
			checkAlignment(alignment);
			checkPointer(newPointer);
		}

		// Align pointer to the specified alignment
		newPointer &= ~(alignment - 1);

		pointer = newPointer;

		return this.address + newPointer;
	}

	/**
	 * Allocates a block of memory on the stack for an array of {@code num} elements, each of them {@code size} bytes long, and initializes all its bits to
	 * zero.
	 *
	 * @param alignment the required element alignment
	 * @param num       num  the number of elements to allocate
	 * @param size      the size of each element
	 *
	 * @return the memory address on the stack for the requested allocation
	 */
	public long ncalloc(int alignment, int num, int size) {
		int bytes = num * size;
		long address = nmalloc(alignment, bytes);
		memSet(address, 0, bytes);
		return address;
	}

	// -------------------------------------------------

	/**
	 * Allocates a {@link ByteBuffer} on the stack.
	 *
	 * @param size the number of elements in the buffer
	 *
	 * @return the allocated buffer
	 */
	public ByteBuffer malloc(int size) { return memByteBuffer(nmalloc(size), size); }
	/** Calloc version of {@link #malloc(int)}. */
	public ByteBuffer calloc(int size) { return memByteBuffer(ncalloc(1, size, 1), size); }

	/** Single value version of {@link #malloc}. */
	public ByteBuffer bytes(byte x) {
		return malloc(1).put(0, x);
	}

	/** Two value version of {@link #malloc}. */
	public ByteBuffer bytes(byte x, byte y) {
		return malloc(2).put(0, x).put(1, y);
	}

	/** Three value version of {@link #malloc}. */
	public ByteBuffer bytes(byte x, byte y, byte z) {
		return malloc(3).put(0, x).put(1, y).put(2, z);
	}

	/** Four value version of {@link #malloc}. */
	public ByteBuffer bytes(byte x, byte y, byte z, byte w) {
		return malloc(4).put(0, x).put(1, y).put(2, z).put(3, w);
	}

	/** Vararg version of {@link #malloc}. */
	public ByteBuffer bytes(byte... values) {
		ByteBuffer buffer = malloc(values.length).put(values);
		buffer.flip();
		return buffer;
	}

	// -------------------------------------------------

	/** Short version of {@link #malloc(int)}. */
	public ShortBuffer mallocShort(int size) { return memShortBuffer(nmalloc(2, size << 1), size); }
	/** Short version of {@link #calloc(int)}. */
	public ShortBuffer callocShort(int size) { return memShortBuffer(ncalloc(2, size, 2), size); }

	/** Single value version of {@link #mallocShort}. */
	public ShortBuffer shorts(short x) { return mallocShort(1).put(0, x); }
	/** Two value version of {@link #mallocShort}. */
	public ShortBuffer shorts(short x, short y) { return mallocShort(2).put(0, x).put(1, y); }
	/** Three value version of {@link #mallocShort}. */
	public ShortBuffer shorts(short x, short y, short z) { return mallocShort(3).put(0, x).put(1, y).put(2, z); }
	/** Four value version of {@link #mallocShort}. */
	public ShortBuffer shorts(short x, short y, short z, short w) { return mallocShort(4).put(0, x).put(1, y).put(2, z).put(3, w); }

	/** Vararg version of {@link #mallocShort}. */
	public ShortBuffer shorts(short... values) {
		ShortBuffer buffer = mallocShort(values.length).put(values);
		buffer.flip();
		return buffer;
	}

	// -------------------------------------------------

	/** Int version of {@link #malloc(int)}. */
	public IntBuffer mallocInt(int size) { return memIntBuffer(nmalloc(4, size << 2), size); }
	/** Int version of {@link #calloc(int)}. */
	public IntBuffer callocInt(int size) { return memIntBuffer(ncalloc(4, size, 4), size); }

	/** Single value version of {@link #mallocInt}. */
	public IntBuffer ints(int x) { return mallocInt(1).put(0, x); }
	/** Two value version of {@link #mallocInt}. */
	public IntBuffer ints(int x, int y) { return mallocInt(2).put(0, x).put(1, y); }
	/** Three value version of {@link #mallocInt}. */
	public IntBuffer ints(int x, int y, int z) { return mallocInt(3).put(0, x).put(1, y).put(2, z); }
	/** Four value version of {@link #mallocInt}. */
	public IntBuffer ints(int x, int y, int z, int w) { return mallocInt(4).put(0, x).put(1, y).put(2, z).put(3, w); }

	/** Vararg version of {@link #mallocInt}. */
	public IntBuffer ints(int... values) {
		IntBuffer buffer = mallocInt(values.length).put(values);
		buffer.flip();
		return buffer;
	}

	// -------------------------------------------------

	/** Long version of {@link #malloc(int)}. */
	public LongBuffer mallocLong(int size) { return memLongBuffer(nmalloc(8, size << 3), size); }
	/** Long version of {@link #calloc(int)}. */
	public LongBuffer callocLong(int size) { return memLongBuffer(ncalloc(8, size, 8), size); }

	/** Single value version of {@link #mallocLong}. */
	public LongBuffer longs(long x) { return mallocLong(1).put(0, x); }
	/** Two value version of {@link #mallocLong}. */
	public LongBuffer longs(long x, long y) { return mallocLong(2).put(0, x).put(1, y); }
	/** Three value version of {@link #mallocLong}. */
	public LongBuffer longs(long x, long y, long z) { return mallocLong(3).put(0, x).put(1, y).put(2, z); }
	/** Four value version of {@link #mallocLong}. */
	public LongBuffer longs(long x, long y, long z, long w) { return mallocLong(4).put(0, x).put(1, y).put(2, z).put(3, w); }

	/** Vararg version of {@link #mallocLong}. */
	public LongBuffer longs(long... more) {
		LongBuffer buffer = mallocLong(more.length).put(more);
		buffer.flip();
		return buffer;
	}

	// -------------------------------------------------

	/** Float version of {@link #malloc(int)}. */
	public FloatBuffer mallocFloat(int size) { return memFloatBuffer(nmalloc(4, size << 2), size); }
	/** Float version of {@link #calloc(int)}. */
	public FloatBuffer callocFloat(int size) { return memFloatBuffer(ncalloc(4, size, 4), size); }

	/** Single value version of {@link #mallocFloat}. */
	public FloatBuffer floats(float x) { return mallocFloat(1).put(0, x); }
	/** Two value version of {@link #mallocFloat}. */
	public FloatBuffer floats(float x, float y) { return mallocFloat(2).put(0, x).put(1, y); }
	/** Three value version of {@link #mallocFloat}. */
	public FloatBuffer floats(float x, float y, float z) { return mallocFloat(3).put(0, x).put(1, y).put(2, z); }
	/** Four value version of {@link #mallocFloat}. */
	public FloatBuffer floats(float x, float y, float z, float w) { return mallocFloat(4).put(0, x).put(1, y).put(2, z).put(3, w); }

	/** Vararg version of {@link #mallocFloat}. */
	public FloatBuffer floats(float... values) {
		FloatBuffer buffer = mallocFloat(values.length).put(values);
		buffer.flip();
		return buffer;
	}

	// -------------------------------------------------

	/** Double version of {@link #malloc(int)}. */
	public DoubleBuffer mallocDouble(int size) { return memDoubleBuffer(nmalloc(8, size << 3), size); }
	/** Double version of {@link #calloc(int)}. */
	public DoubleBuffer callocDouble(int size) { return memDoubleBuffer(ncalloc(8, size, 8), size); }

	/** Single value version of {@link #mallocDouble}. */
	public DoubleBuffer doubles(double x) { return mallocDouble(1).put(0, x); }
	/** Two value version of {@link #mallocDouble}. */
	public DoubleBuffer doubles(double x, double y) { return mallocDouble(2).put(0, x).put(1, y); }
	/** Three value version of {@link #mallocDouble}. */
	public DoubleBuffer doubles(double x, double y, double z) { return mallocDouble(3).put(0, x).put(1, y).put(2, z); }
	/** Four value version of {@link #mallocDouble}. */
	public DoubleBuffer doubles(double x, double y, double z, double w) { return mallocDouble(4).put(0, x).put(1, y).put(2, z).put(3, w); }

	/** Vararg version of {@link #mallocDouble}. */
	public DoubleBuffer doubles(double... values) {
		DoubleBuffer buffer = mallocDouble(values.length).put(values);
		buffer.flip();
		return buffer;
	}

	// -------------------------------------------------

	/** Pointer version of {@link #malloc(int)}. */
	public PointerBuffer mallocPointer(int size) { return memPointerBuffer(nmalloc(POINTER_SIZE, size << POINTER_SHIFT), size); }
	/** Pointer version of {@link #calloc(int)}. */
	public PointerBuffer callocPointer(int size) { return memPointerBuffer(ncalloc(POINTER_SIZE, size, POINTER_SHIFT), size); }

	/** Single value version of {@link #mallocPointer}. */
	public PointerBuffer pointers(long x) { return mallocPointer(1).put(0, x); }
	/** Two value version of {@link #mallocPointer}. */
	public PointerBuffer pointers(long x, long y) { return mallocPointer(2).put(0, x).put(1, y); }
	/** Three value version of {@link #mallocPointer}. */
	public PointerBuffer pointers(long x, long y, long z) { return mallocPointer(3).put(0, x).put(1, y).put(2, z); }
	/** Four value version of {@link #mallocPointer}. */
	public PointerBuffer pointers(long x, long y, long z, long w) { return mallocPointer(4).put(0, x).put(1, y).put(2, z).put(3, w); }

	/** Vararg version of {@link #mallocPointer}. */
	public PointerBuffer pointers(long... values) {
		PointerBuffer buffer = mallocPointer(values.length).put(values);
		buffer.flip();
		return buffer;
	}

	/** Single value version of {@link #mallocPointer}. */
	public PointerBuffer pointers(Pointer x) { return mallocPointer(1).put(0, x); }
	/** Two value version of {@link #mallocPointer}. */
	public PointerBuffer pointers(Pointer x, Pointer y) { return mallocPointer(2).put(0, x).put(1, y); }
	/** Three value version of {@link #mallocPointer}. */
	public PointerBuffer pointers(Pointer x, Pointer y, Pointer z) { return mallocPointer(3).put(0, x).put(1, y).put(2, z); }
	/** Four value version of {@link #mallocPointer}. */
	public PointerBuffer pointers(Pointer x, Pointer y, Pointer z, Pointer w) { return mallocPointer(4).put(0, x).put(1, y).put(2, z).put(3, w); }

	/** Vararg version of {@link #mallocPointer}. */
	public PointerBuffer pointers(Pointer... values) {
		PointerBuffer buffer = mallocPointer(values.length);
		for ( int i = 0; i < values.length; i++ )
			buffer.put(i, values[i]);
		return buffer;
	}

}