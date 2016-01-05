package com.woting.common.model;

import com.spiritdata.framework.core.model.BaseObject;

/**
 * 所有者对象，封装了ownerId和ownerType
 * @author wh
 */
public class Owner extends BaseObject {
    private static final long serialVersionUID = -1970271589243412626L;

    private int ownerType; //模式所对应的所有者类型（1=注册用户;2=非注册用户(session)）
    private String ownerId; //所有者标识（可能是用户id，也可能是SessionID）

    public Owner() {
        super();
    }

    public int getOwnerType() {
        return ownerType;
    }

    public Owner(int ownerType, String ownerId) {
        super();
        this.ownerType = ownerType;
        this.ownerId = ownerId;
    }

    public Owner(String ownerId, int ownerType) {
        super();
        this.ownerType = ownerType;
        this.ownerId = ownerId;
    }
    
    public void setOwnerType(int ownerType) {
        this.ownerType = ownerType;
    }
    public String getOwnerId() {
        return ownerId;
    }
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        Owner other = (Owner)obj;
        if (ownerId==null&&other.ownerId!=null) return false;
        else if (!ownerId.equals(other.ownerId)) return false;
        if (ownerType!=other.ownerType) return false;
        return true;
    }

	@Override
	public int hashCode() {
		int retInt = 0;
		String str = this.ownerId+":"+this.ownerType;
		for(int i=0;i<str.length();i++){
			char achar = str.charAt(i);
			retInt += achar;
		}
		return retInt;
	}
}