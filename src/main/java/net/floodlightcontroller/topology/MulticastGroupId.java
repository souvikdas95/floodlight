package net.floodlightcontroller.topology;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPAddress;

public class MulticastGroupId {
	private final IPAddress<?> groupAddress;
	private final DatapathId archId;
	
	public MulticastGroupId(IPAddress<?> groupAddress, DatapathId archId) {
		this.groupAddress = groupAddress;
		this.archId = archId;
	}
	
	public IPAddress<?> getGroupAddress() {
		return groupAddress;
	}
	
	public DatapathId getArchipelagoId() {
		return archId;
	}
	
    @Override
    public boolean equals(Object o) {
        if (this == o) {
        	return true;
        }
        
        if (o == null || getClass() != o.getClass()) {
        	return false;
        }

        MulticastGroupId that = (MulticastGroupId) o;
       
        if (groupAddress == null || that.groupAddress == null || 
        		!groupAddress.equals(that.groupAddress)) {
        	return false;
        }
        
        if (archId == null || that.archId == null || 
        		!archId.equals(that.archId)) {
        	return false;
        }
        
        return true;
    }

    @Override
    public int hashCode() {
        int result = groupAddress.hashCode();
        result = 31 * result + archId.hashCode();
        return result;
    }
}
