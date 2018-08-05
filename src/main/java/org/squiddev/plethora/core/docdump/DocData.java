package org.squiddev.plethora.core.docdump;

import com.google.common.base.Strings;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

public class DocData<T> implements Comparable<MethodData> {
	private static final char[] terminators = new char[]{'!', '.', '?', '\r', '\n'};

	/**
	 * The class this object targets
	 */
	@Nonnull
	public final String target;

	/**
	 * The "friendly" display name for this object
	 */
	@Nonnull
	public final String name;

	/**
	 * The object we're representing
	 */
	@Nonnull
	@JsonAdapter(ClassNameAdapter.class)
	public final T value;

	/**
	 * A brief summary of the object
	 */
	@Nullable
	public final String synopsis;

	/**
	 * The remaining description after the synopsis
	 */
	@Nullable
	public final String detail;

	public DocData(@Nonnull Class<?> target, @Nonnull T value, @Nonnull String name, @Nullable String doc) {
		this.target = target.getName();
		this.value = value;
		this.name = name;

		if (doc != null) {
			String synopsis, detail = null;

			// Get minimum position
			int position = -1;
			for (char chr : terminators) {
				int newPos = doc.indexOf(chr);
				if (position == -1 || (newPos > -1 && newPos < position)) position = newPos;
			}

			if (position > -1) {
				synopsis = doc.substring(0, position).trim();
				detail = doc.substring(position + 1).trim();
			} else if (doc.length() > 80) {
				synopsis = doc.substring(0, 77).trim() + "...";
				detail = doc;
			} else {
				synopsis = doc;
			}

			this.synopsis = Strings.isNullOrEmpty(synopsis) ? null : synopsis;
			this.detail = Strings.isNullOrEmpty(detail) ? null : detail;

		} else {
			this.synopsis = null;
			this.detail = null;
		}
	}

	@Override
	public int compareTo(@Nonnull MethodData o) {
		return name.compareTo(o.name);
	}

	private static class ClassNameAdapter extends TypeAdapter {

		@Override
		public void write(JsonWriter out, Object value) throws IOException {
			if (value == null) {
				out.nullValue();
			} else {
				out.value(value.getClass().getName());
			}
		}

		@Override
		public Object read(JsonReader in) {
			return null;
		}
	}
}
