package com.ibm.rdf.store.sparql11.model;

/**
 * Represents a blank node
 */
public class BlankNode {
	
	private static int BNODE_ID = 1;
	
	private String label = null;
	
	public BlankNode() { this("brstore" + (BNODE_ID++)); }
	
	public BlankNode(String label) { this.label = label; }
	
	public String getLabel() { return label; }
	public boolean hasLabel() { return label != null; }
	
	public String toString() { 
		if(hasLabel()) return "_:" + getLabel();
		else return "()";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BlankNode other = (BlankNode) obj;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		return true;
	}
	
	
}
