package com.lang.buffer;

import java.util.Arrays;

public abstract class ObjectBuffer<T> {

	private final Object[] buffer;
	private final int forwardWindow;
	private final int backwardWindow;

	protected ObjectHandler<T> handler;

	private int head = 0;
	private int tail = -1;
	private boolean hitEof = false;
	private int eofIndex = -1;

	protected ObjectBuffer(int forwardWindow, int backwardWindow, int capacityMultiplier) {
		if (forwardWindow < 0 || backwardWindow < 0 || capacityMultiplier <= 0) {
			throw new IllegalArgumentException(
					"forwardWindow, backwardWindow must be >= 0 and capacityMultiplier > 0");
		}
		this.forwardWindow = forwardWindow;
		this.backwardWindow = backwardWindow;

		int windowSize = backwardWindow + forwardWindow;
		int capacity = (windowSize * capacityMultiplier) + 1;

		this.buffer = new Object[Math.max(capacity, windowSize)];
	}

	protected abstract T fetchNext();

	protected abstract boolean isEOF(T element);

	protected void maintainWindow() {
		int neededPhysicalIndex = head + forwardWindow;

		if (neededPhysicalIndex >= buffer.length && !hitEof) {
			int startCopySource = Math.max(0, head - backwardWindow);
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

		while (tail < (head + forwardWindow) && !hitEof) {
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
		if (index < -backwardWindow || index > forwardWindow) {
			throw new IndexOutOfBoundsException(
					"Index " + index + " out of allowed window ["
							+ (-backwardWindow) + ", " + forwardWindow + "]");
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