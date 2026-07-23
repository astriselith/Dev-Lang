package com.lang.buffer;

import java.util.Arrays;

public abstract class ObjectBuffer<T> {

	private final Object[] buffer;
	private final int sideWindow;

	protected ObjectHandler<T> handler;

	private int head = 0;
	private int tail = -1;
	private boolean hitEof = false;
	private int eofIndex = -1;

	protected ObjectBuffer(int sideWindow) {
		if (sideWindow <= 0) {
			throw new IllegalArgumentException(
					"sidedWindow must be > 0");
		}
		this.sideWindow = Math.max(sideWindow, 3);
		int capacity = (this.sideWindow * 2) + 1;

		this.buffer = new Object[capacity];
	}

	protected abstract T fetchNext();

	protected abstract boolean isEOF(T element);

	protected void maintainWindow() {
		int neededPhysicalIndex = head + sideWindow;

		if (neededPhysicalIndex >= buffer.length && !hitEof) {
			int startCopySource = Math.max(0, head - sideWindow);
			int elementsToKeep = (tail - startCopySource) + 1;

			if (elementsToKeep > 0) {
				System.arraycopy(buffer, startCopySource, buffer, 0, elementsToKeep);
			}

			int displacement = startCopySource;
			this.head -= displacement;
			this.tail -= displacement;
			if (eofIndex >= 0)
				eofIndex -= displacement;

			for (int i = Math.max(0, elementsToKeep); i < buffer.length; i++) {
				buffer[i] = null;
			}
		}

		while (tail < (head + sideWindow) && !hitEof) {
			if (tail + 1 >= buffer.length) {
				break;
			}

			T next = fetchNext();

			if (handler != null && !isEOF(next)) {
				if (!handler.handle(next)) {
					continue;
				}
			}

			if (next == null || isEOF(next)) {
				tail++;
				buffer[tail] = null;
				hitEof = true;
				eofIndex = tail;
				break;
			}

			tail++;
			buffer[tail] = next;
		}
	}

	@SuppressWarnings("unchecked")
	public T offset(int index) {
		if (index < -sideWindow || index > sideWindow) {
			throw new IndexOutOfBoundsException(
					"Index " + index + " out of allowed window ["
							+ (-sideWindow) + ", " + sideWindow + "]");
		}

		maintainWindow();

		int target = head + index;

		if (target < 0) {
			throw new IndexOutOfBoundsException("Index " + target + " is negative (outside history).");
		}

		if (target > tail) {
			if (hitEof) {
				return null;
			}
			throw new IndexOutOfBoundsException(
					"Index " + target + " not yet loaded or out of bounds. Tail: " + tail);
		}

		return (T) buffer[target];
	}

	public T next() {
		T current = offset(0);
		if (current != null && !isEOF(current)) {
			head++;
		}
		return current;
	}

	public boolean hasNext() {
		maintainWindow();
		if (hitEof && head >= eofIndex) {
			return false;
		}
		return head <= tail && buffer[head] != null;
	}

	public ObjectHandler<T> getHandler() {
		return handler;
	}

	public void setHandler(ObjectHandler<T> handler) {
		this.handler = handler;
	}

	public int getHead() {
		return head;
	}

	public int getTail() {
		return tail;
	}

	public int getBufferSize() {
		return buffer.length;
	}

	public void release() {
		Arrays.fill(buffer, null);
		head = 0;
		tail = -1;
		hitEof = false;
		eofIndex = -1;
	}
}