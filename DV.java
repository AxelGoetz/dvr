import java.lang.Math;
import java.util.Vector;
import java.util.Collections;

public class DV implements RoutingAlgorithm {

    static int LOCAL = -1;
    static int UNKNOWN = -2;
    static int INFINITY = 60;

    private Router router;
    private int updateInterval;
    private boolean allowPReverse;
    private boolean allowExpire;

    private int timeout;
    private int garbageCollection;

    private Vector<DVRoutingTableEntry> routingTable;

    public DV() {
        routingTable = new Vector<DVRoutingTableEntry>();
    }

    public void setRouterObject(Router obj) {
        this.router = obj;
    }

    public void setUpdateInterval(int u) {
        this.updateInterval = u;
        this.timeout = 4 * u;
        this.garbageCollection = 3 * u;
    }

    public void setAllowPReverse(boolean flag) {
        this.allowPReverse = flag;
    }

    public void setAllowExpire(boolean flag) {
        this.allowExpire = flag;
    }

    public void initalise() {
        routingTable.add(new DVRoutingTableEntry(router.getId(), LOCAL, 0, router.getCurrentTime()));
    }

    /**
     * Loops through the routing table and returns the DVRoutingTableEntry
     * that matches the destination id.
     */
    private DVRoutingTableEntry lookup(int destination) {
        for(DVRoutingTableEntry entry: routingTable) {
            if(entry.getDestination() == destination) {
                return entry;
            }
        }
        return null;
    }

    /**
     * For a given destination address, returns the appriopriate interface
     * to send the message to.
     * @return the local interface
     */
    public int getNextHop(int destination) {
        DVRoutingTableEntry entry = lookup(destination);
        if(entry == null || entry.getMetric() == INFINITY) {
            return UNKNOWN;
        }
        return entry.getInterface();
    }

    /**
     * If a particular interface fails, this method updates the routing table
     * and sets all values with that interface to infinity.
     * @param iface the id of the interface
     */
    private void setInterfaceToInfinity(int iface) {
        for(DVRoutingTableEntry entry: routingTable) {
            if(entry.getInterface() == iface) {
                entry.setMetric(INFINITY);

                // Since the entry is updated, restart the garbage collection timer
                // entry.setTime(router.getCurrentTime());
            }
        }
    }

    public void tidyTable() {
        for(int i = 0; i < router.getNumInterfaces(); i++) {
            if(!router.getInterfaceState(i)) {
                setInterfaceToInfinity(i);
            }
        }

        if(allowExpire) {
            int time = router.getCurrentTime();

            for(int i = 0; i < routingTable.size(); i++) {
                DVRoutingTableEntry entry = routingTable.get(i);
                if(entry.getInterface() == LOCAL) continue;

                int diff = time - entry.getTime();

                if(entry.getMetric() == INFINITY && diff > garbageCollection) {
                    routingTable.remove(i);
                    i -= 1;
                } /*else if(diff > timeout) {
                    entry.setMetric(INFINITY);
                }*/
            }
        }
    }

    /**
     * Given an interface, generates the appripriate routing packet payload.
     * @param iface the id of the interface
     */
    private Payload getPayLoadRoutingPacket(int iface) {
        Payload payload = new Payload();
        for(DVRoutingTableEntry entry: routingTable) {
            // If preverse is turned on, you cannot announce the route where the destination is in on the interface used as the next hop
            // towards that destination. So we set the metric to infinity (poison reverse)
            // It doesn't matter if you use `entry.getTime()` or `router.getCurrentTime()` because when a router received a packet,
            // it will modify the time anyway.
            if(allowPReverse && getNextHop(entry.getDestination()) == iface) {
              payload.addEntry(new DVRoutingTableEntry(entry.getDestination(), entry.getInterface(), INFINITY, entry.getTime()));
            } else {
              payload.addEntry(new DVRoutingTableEntry(entry.getDestination(), entry.getInterface(), entry.getMetric(), entry.getTime()));
            }
        }
        return payload;
    }

    /**
     * If an interface is up, it generates a routing packet and sends it
     * @return routing packet or null if the interface is down
     */
    public Packet generateRoutingPacket(int iface) {
        int time = router.getCurrentTime();

        if(router.getInterfaceState(iface)) {
            Payload payload = getPayLoadRoutingPacket(iface);
            Packet packet = new RoutingPacket(router.getId(), Packet.BROADCAST);
            packet.setPayload(payload);

            return packet;
        }
        return null;
    }

    /**
     * Given a new entry, this method adds it to the routingTable.
     */
    private void addEntryToTable(DVRoutingTableEntry entry) {
        DVRoutingTableEntry currEntry = lookup(entry.getDestination());

        // Entry doesn't exist yet, so we add it
        if(currEntry == null) {
            // Don't add a new entry with metric infinity
            if(allowExpire && entry.getMetric() == INFINITY) return;
            entry.setTime(router.getCurrentTime());
            routingTable.add(entry);
        } // Entries have the same interface so we update no matter what value it is
        else if(entry.getInterface() == currEntry.getInterface()) {
            // If the current entry was already infinity, we don't want to update the time
            if(allowExpire && currEntry.getMetric() == INFINITY && entry.getMetric() == INFINITY) return;
            currEntry.setTime(router.getCurrentTime());
            currEntry.setMetric(entry.getMetric());
        } // Finally, if the metric is better, update
        else if(entry.getMetric() < currEntry.getMetric()) {
            currEntry.setTime(router.getCurrentTime());
            currEntry.setMetric(entry.getMetric());
            currEntry.setInterface(entry.getInterface());
        }
    }

    /**
     * Given a routing packet p that came in on interface iface,
     * it processes the packet and updates the routing table.
     */
    public void processRoutingPacket(Packet p, int iface) {
        Vector<Object> payload=p.getPayload().getData();

        for(Object obj: payload) {
            DVRoutingTableEntry entry = (DVRoutingTableEntry)obj;

            int metric = router.getInterfaceWeight(iface) + entry.getMetric();
            if(metric > INFINITY) metric = INFINITY;
            entry.setMetric(metric);
            entry.setInterface(iface);

            addEntryToTable(entry);
        }
        Collections.sort(routingTable);
    }

    /**
     * Prints the routing table out in the provided format
     */
    public void showRoutes() {
        System.out.println("Router " + router.getId());
        for(RoutingTableEntry entry : routingTable) {
            System.out.println(entry.toString());
        }
    }
}

class DVRoutingTableEntry implements RoutingTableEntry, Comparable<DVRoutingTableEntry> {
    private int destination;
    private int iface;
    private int metric;
    private int time;

    public DVRoutingTableEntry(int d, int i, int m, int t) {
        this.destination = d;
        this.iface = i;
        this.metric = m;
        this.time = t;
    }

    public int getDestination() { return this.destination; }

    public void setDestination(int d) { this.destination = d; }

    public int getInterface() { return this.iface; }

    public void setInterface(int i) { this.iface = i; }

    public int getMetric() { return this.metric; }

    public void setMetric(int m) {this.metric = m; }

    public int getTime() { return this.time; }

    public void setTime(int t) { this.time = t; }

    public String toString() {
        return "d " + this.getDestination() + " i " + this.getInterface() + " m " + this.getMetric();
    }

    /**
     * To sort the entries
     */
    public int compareTo(DVRoutingTableEntry entry){
        return this.getDestination() - entry.getDestination();
    }
}
