package net.floodlightcontroller.routing;

import java.math.BigInteger;

import org.projectfloodlight.openflow.types.DatapathId;

public class MulticastPathId implements Cloneable, Comparable<MulticastPathId> {
    protected DatapathId src;
    protected BigInteger mgId;

    public MulticastPathId(DatapathId src, BigInteger mgId) {
        super();
        this.src = src;
        this.mgId = mgId;
    }

    public DatapathId getSrc() {
        return src;
    }

    public void setSrc(DatapathId src) {
        this.src = src;
    }

    public BigInteger getMgId() {
        return mgId;
    }

    public void setMgId(BigInteger mgId) {
        this.mgId = mgId;
    }

    @Override
    public int hashCode() {
        final int prime = 2417;
        Long result = new Long(1);
        result = prime * result + ((mgId == null) ? 0 : mgId.hashCode());
        result = prime * result + ((src == null) ? 0 : src.hashCode());
        return result.hashCode(); 
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MulticastPathId other = (MulticastPathId) obj;
        if (mgId == null) {
            if (other.mgId != null)
                return false;
        } else if (!mgId.equals(other.mgId))
            return false;
        if (src == null) {
            if (other.src != null)
                return false;
        } else if (!src.equals(other.src))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "RouteId [src=" + this.src.toString() + " mgId="
                + this.mgId.toString() + "]";
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public int compareTo(MulticastPathId o) {
        int result = src.compareTo(o.getSrc());
        if (result != 0)
            return result;
        return mgId.compareTo(o.getMgId());
    }
}