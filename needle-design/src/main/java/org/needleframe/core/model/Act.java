package org.needleframe.core.model;

public enum Act {
	READ(1 << 0),
	
	CREATE(1 << 1 | READ.mask),
	
	UPDATE(1 << 2 | READ.mask),
	
	DELETE(1 << 3 | READ.mask),

	IMPORT(1 << 4 | READ.mask),
	
	EXPORT(1 << 5 | READ.mask),
	
	EXECTE(1 << 6 | READ.mask);
	
	private int mask;
	
	private Act(int mask) {
		this.mask = mask;
	}
	
	public int getMask() {
		return this.mask;
	}
	
	public static int CRUD() {
		return CREATE.mask | UPDATE.mask | DELETE.mask;
	}
	
	public static int IE() {
		return IMPORT.mask | EXPORT.mask;
	}
	
	public static int getAll() {
		return CRUD() | IE();
	}
}
