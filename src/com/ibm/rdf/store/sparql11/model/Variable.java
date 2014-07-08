package com.ibm.rdf.store.sparql11.model;

/**
 * models a variable
 */
public class Variable {
   private boolean isSystemVariable;
	private String name;
	
	public Variable(String name) {
		assert ! name.contains(":");
		this.name = name;
		this.isSystemVariable = false;
	}
	
	public Variable(String name, boolean isSystemVariable) {
     assert ! name.contains(":");
     this.name = name;
     this.isSystemVariable = isSystemVariable;
  }

	public boolean isSystemVariable()
	   {
	   return isSystemVariable;
	   }

   public String getName() { return name; }
	
	public String toString() { return "?" + name; }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		Variable other = (Variable) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	
}
