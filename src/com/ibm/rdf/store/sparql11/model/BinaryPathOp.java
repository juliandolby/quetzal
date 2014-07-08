package com.ibm.rdf.store.sparql11.model;
/**
 * 
 *
 */
public abstract class BinaryPathOp extends Path {
	protected Path left;
	protected Path right;
	public BinaryPathOp(Path left, Path right) {
		super();
		this.left = left;
		this.right = right;
	}
	public Path getLeft() {
		return left;
	}
	public Path getRight() {
		return right;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((left == null) ? 0 : left.hashCode());
		result = prime * result + ((right == null) ? 0 : right.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BinaryPathOp other = (BinaryPathOp) obj;
		if (left == null) {
			if (other.left != null)
				return false;
		} else if (!left.equals(other.left))
			return false;
		if (right == null) {
			if (other.right != null)
				return false;
		} else if (!right.equals(other.right))
			return false;
		return true;
	}
	
}
