/**
 * Created by geddingsbarrineau on 4/1/16.
 */
package net.floodlightcontroller.topology;

import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.routing.BroadcastTree;
import org.projectfloodlight.openflow.types.DatapathId;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Archipelago {
	private DatapathId id; // the lowest id of the nodes
    private final Set<Cluster> clusters;
    private BroadcastTree destinationRootedFullTree;
    private Set<MulticastGroup> multicastGroups;

    public Archipelago() {
        id = DatapathId.NONE;
        clusters = new HashSet<Cluster>();
        multicastGroups = new HashSet<MulticastGroup>();
    }
    
    public DatapathId getId() {
        return id;
    }

    protected Set<Cluster> getClusters() {
        return clusters;
    }

    Archipelago add(Cluster c) {
        if (clusters.add(c)) {
            if (id.equals(DatapathId.NONE) || c.getId().compareTo(id) < 0) {
                id = c.getId();
            }
        }
        return this;
    }

    boolean isMember(Cluster c) {
        return clusters.contains(c);
    }

    boolean isMember(DatapathId id) {
        for (Cluster c : clusters) {
            if(c.getNodes().contains(id)) return true;
        }
        return false;
    }

    void merge(Archipelago a) {
        clusters.addAll(a.getClusters());
        if (id.equals(DatapathId.NONE) || !a.getId().equals(DatapathId.NONE) || a.getId().compareTo(id) < 0) {
            id = a.getId();
        }
    }
    
    Set<DatapathId> getSwitches() {
        Set<DatapathId> allSwitches = new HashSet<DatapathId>();
        for (Cluster c : clusters) {
            for (DatapathId d : c.getNodes()) {
                allSwitches.add(d);
            }
        }
        return allSwitches;
    }

    BroadcastTree getBroadcastTree() {
        return destinationRootedFullTree;
    }

    void setBroadcastTree(BroadcastTree bt) {
        destinationRootedFullTree = bt;
    }
    
    void addMulticastGroup(MulticastGroup mg) {
    	removeMulticastGroup(mg);
    	multicastGroups.add(mg);
    }
    
    void removeMulticastGroup(MulticastGroup mg) {
    	if (multicastGroups.contains(mg)) {
    		multicastGroups.remove(mg);
    	}
    	else {
	    	/*
	    	 *  TODO: Reconsider
	    	 *  Just a precaution since underlying
	    	 *  elements are prone to changes. So,
	    	 *  we need to remove it securely.
	    	 */
	    	removeMulticastGroup(mg.getId());
    	}
    }
    
    void removeMulticastGroup(DatapathId mgId) {
    	for (MulticastGroup mg: multicastGroups) {
    		if (mg.getId() == mgId) {
    			multicastGroups.remove(mg);
    			break;
    		}
    	}
    }
    
    boolean hasMulticastGroup(MulticastGroup mg) {
    	if (multicastGroups.contains(mg)) {
    		return true;
    	}
    	/*
    	 *  TODO: Reconsider
    	 *  Just a precaution since underlying
    	 *  elements are prone to changes. So,
    	 *  we need to remove it securely.
    	 */
		return hasMulticastGroup(mg.getId());
    }
    
    boolean hasMulticastGroup(DatapathId mgId) {
    	for (MulticastGroup mg: multicastGroups) {
    		if (mg.getId() == mgId) {
    			return true;
    		}
    	}
    	return false;
    }
    
    MulticastGroup getMulticastGroup(DatapathId mgId) {
    	for (MulticastGroup mg: multicastGroups) {
    		if (mg.getId() == mgId) {
    			return mg;
    		}
    	}
    	return null;
    }
    
    Set<MulticastGroup> getMulticastGroups() {
    	return Collections.unmodifiableSet(multicastGroups);
    }
    
    void clearMulticastGroups() {
    	multicastGroups.clear();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Archipelago that = (Archipelago) o;

        if (!id.equals(that.id)) return false;
        return clusters.equals(that.clusters);

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + clusters.hashCode();
        return result;
    }

    public String toString() {
        return "[Archipelago id=" + id.toString() + ", " + clusters.toString() + "]";
    }
}
