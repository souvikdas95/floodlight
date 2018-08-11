package net.floodlightcontroller.routing;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import net.floodlightcontroller.core.types.NodePortTuple;
import net.floodlightcontroller.routing.web.serializers.PathSerializer;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.U64;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@JsonSerialize(using=PathSerializer.class)
public class MulticastPath implements Comparable<MulticastPath> {
    protected MulticastPathId id;
    protected List<NodePortTuple> switchPorts;
    protected int pathIndex;
    protected int hopCount;
    protected U64 latency;
    protected int cost;

    public MulticastPath(MulticastPathId id, List<NodePortTuple> switchPorts) {
        super();
        this.id = id;
        this.switchPorts = switchPorts;
        this.pathIndex = 0; // useful if multipath routing available
    }

    public MulticastPath(DatapathId src, BigInteger mgId) {
        super();
        this.id = new MulticastPathId(src, mgId);
        this.switchPorts = new ArrayList<NodePortTuple>();
        this.pathIndex = 0;
    }

    /**
     * @return the id
     */
    public MulticastPathId getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(MulticastPathId id) {
        this.id = id;
    }

    /**
     * @return the path
     */
    public List<NodePortTuple> getPath() {
        return switchPorts;
    }

    /**
     * @param path the path to set
     */
    public void setPath(List<NodePortTuple> switchPorts) {
        this.switchPorts = switchPorts;
    }

    /**
     * @param pathIndex pathIndex
     */
    public void setPathIndex(int pathIndex) {
        this.pathIndex = pathIndex;
    }
    
    /**
     * @return pathIndex
     */
    public int getPathIndex() {
        return pathIndex;
    }

    public void setHopCount(int hopCount) { 
        this.hopCount = hopCount; 
    }

    public int getHopCount() { 
        return this.hopCount;
    }

    public void setLatency(U64 latency) { 
        this.latency = latency; 
    }

    public U64 getLatency() { 
        return this.latency; 
    }
    
    public void setCost(int cost) {
    	this.cost = cost;
    }
    
    public int getCost() {
    	return this.cost;
    }
    
    @Override
    public int hashCode() {
        final int prime = 5791;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((switchPorts == null) ? 0 : switchPorts.hashCode());
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
        MulticastPath other = (MulticastPath) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (switchPorts == null) {
            if (other.switchPorts != null)
                return false;
        } else if (!switchPorts.equals(other.switchPorts))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Route [id=" + id + ", switchPorts=" + switchPorts + "]";
    }

    /**
     * Compares the path lengths.
     */
    @Override
    public int compareTo(MulticastPath o) {
        return ((Integer)switchPorts.size()).compareTo(o.switchPorts.size());
    }
}
