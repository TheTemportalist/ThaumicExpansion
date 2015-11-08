package com.temportalist.thaumicexpansion.common.lib;

/**
 * @author TheTemportalist
 */
public class Pair<A, B> {

	private A key;
	private B value;

	public Pair(A key, B value) {
		this.key = key;
		this.value = value;
	}

	public A getKey() {
		return key;
	}

	public B getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		int hash = 1;
		hash = hash * 37 + (this.key == null ? 0 : this.key.hashCode());
		hash = hash * 37 + (this.value == null ? 0 : this.value.hashCode());
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Pair && this.key.equals(((Pair) obj).key) &&
				this.value.equals(((Pair) obj).value);
	}

}
