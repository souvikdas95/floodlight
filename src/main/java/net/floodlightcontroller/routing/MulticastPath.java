package net.floodlightcontroller.routing;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableSet;

import net.floodlightcontroller.routing.web.serializers.PathSerializer;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U64;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@JsonSerialize(using=PathSerializer.class)
public class MulticastPath implements Comparable<MulticastPath> {
    protected MulticastPathId id;
    protected Map<DatapathId, Path> mgSwIdPathMap;	// Map of mgSwId and Path it belongs to
    protected Map<DatapathId, Set<OFPort>> mgSwIdAPMap;	// Map of mgSwId and Set of attachmentPointPorts
    protected int pathIndex;

    public MulticastPath(MulticastPathId id) {
        super();
        this.id = id;  
        this.mgSwIdPathMap = new HashMap<DatapathId, Path>();
        this.mgSwIdAPMap = new HashMap<DatapathId, Set<OFPort>>();
        this.pathIndex = 0; // useful if multipath multicast routing available
    }

    public MulticastPath(DatapathId src, BigInteger mgId) {
        super();
        this.id = new MulticastPathId(src, mgId);
        this.mgSwIdPathMap = new HashMap<DatapathId, Path>();
        this.mgSwIdAPMap = new HashMap<DatapathId, Set<OFPort>>();
        this.pathIndex = 0; // useful if multipath multicast routing available
    }

    public MulticastPathId getId() {
        return id;
    }

    public void setId(MulticastPathId id) {
        this.id = id;
    }
    
    public void add(DatapathId mgSwId, Set<OFPort> attachmentPointPorts, Path path) {
    	mgSwIdPathMap.put(mgSwId, path);
    	mgSwIdAPMap.put(mgSwId, attachmentPointPorts);
    }
    
    public void remove(DatapathId mgSwId) {
    	mgSwIdPathMap.remove(mgSwId);
    	mgSwIdAPMap.remove(mgSwId);
    }
    
    public void remove(Path path) {
    	DatapathId mgSwId = path.getId().getDst();
    	mgSwIdPathMap.remove(mgSwId);
    	mgSwIdAPMap.remove(mgSwId);
    }
    
    public DatapathId getRoot() {
    	return id.getSrc();
    }
    
    public BigInteger getMgId() {
    	return id.getMgId();
    }
    
    public Collection<Path> getAllPaths() {
        return Collections.unmodifiableCollection(mgSwIdPathMap.values());
    }

    public Set<DatapathId> getAllMgSwIds() {
    	 return Collections.unmodifiableSet(mgSwIdPathMap.keySet());
    }
    
    public Path getPath(DatapathId mgSwId) {
    	return mgSwIdPathMap.get(mgSwId);
    }
    
    public DatapathId getMgSwId(Path path) {
    	DatapathId mgSwId = path.getId().getDst();
    	return mgSwId;
    }
    
    public Set<OFPort> getAttachmentPoints(DatapathId mgSwId) {
    	return Collections.unmodifiableSet(mgSwIdAPMap.get(mgSwId));
    }
    
    public boolean hasPath(Path path) {
    	DatapathId mgSwId = path.getId().getDst();
    	return mgSwIdPathMap.containsKey(mgSwId);
    }
    
    public boolean hasMgSwId(DatapathId mgSwId) {
    	return mgSwIdPathMap.containsKey(mgSwId);
    }
    
    public boolean isEmpty() {
    	return mgSwIdPathMap.isEmpty();
    }
    
    public void clear() {
    	mgSwIdPathMap.clear();
    }
    
    public int getMulticastPathIndex() {
        return pathIndex;
    }
    
    public void setMulticastPathIndex(int pathIndex) {
        this.pathIndex = pathIndex;
    }

    public int getHopCount() { 
        int hopCount = 0;
        for (Path path: mgSwIdPathMap.values()) {
        	hopCount += path.getHopCount();
        }
        return hopCount;
    }

    public U64 getLatency() { 
    	U64 latency = U64.ZERO;
        for (Path path:mgSwIdPathMap.values()) {
        	latency.add(path.getLatency());
        }
        return latency;
    }
    
    public int getCost() {
        int cost = 0;
        for (Path path: mgSwIdPathMap.values()) {
        	cost += path.getCost();
        }
        return cost;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + pathIndex;
        result = prime * result + mgSwIdPathMap.hashCode();
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
        if (id == null || other.id == null ||
			!id.equals(other.id)) {
        	return false;
        }
        if (pathIndex != other.pathIndex) {
        	return false;
        }
        if (!mgSwIdPathMap.equals(other.mgSwIdPathMap)) {
        	return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Route [id=" + id + ", paths=" + mgSwIdPathMap.values() + "]";
    }

    @Override
    public int compareTo(MulticastPath o) {
        return ((Integer)mgSwIdPathMap.size()).compareTo(o.mgSwIdPathMap.size());
    }
}
